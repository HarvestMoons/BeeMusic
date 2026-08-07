package com.example.musicplayer.task;

import com.example.musicplayer.model.Song;
import com.example.musicplayer.model.SongVote;
import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.repository.SongVoteRepository;
import com.example.musicplayer.service.RedisTaskLock;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteCountSyncTask {

    private final SongRepository songRepository;
    private final SongVoteRepository songVoteRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisTaskLock taskLock;

    private static final String LOCK_KEY = "music:task-lock:vote-sync";
    private static final String REBUILD_MARKER = "music:votes:redis-rebuilt";

    /**
     * 定时把 Redis 实时票数回写到数据库 songs.like_count / dislike_count
     * 加上这个任务后：就算 Redis 整个挂掉/重启/清空，票数也永远不会丢
     */
    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.MINUTES) // 每60分钟执行一次
    // 也可以用 cron： @Scheduled(cron = "0 */5 * * * ?")
    public void syncVoteCountsFromRedisToDatabase() {
        String token = taskLock.tryAcquire(LOCK_KEY, Duration.ofHours(2));
        if (token == null) {
            log.info("【投票同步任务】已有其他实例执行，跳过本次任务");
            return;
        }
        log.info("【投票同步任务】开始执行 Redis → MySQL 票数回写");
        try {
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(REBUILD_MARKER))) {
                log.warn("【投票同步任务】Redis 投票集合尚未从数据库重建，跳过回写以避免清零持久化票数");
                rebuildRedisVotes();
            }
            syncCounts();
        } finally {
            taskLock.release(LOCK_KEY, token);
        }
    }

    /**
     * 项目启动时强制同步一次（防止 Redis 冷启动后票数为0）
     */
    @PostConstruct // 启动时也同步一次
    public void syncOnStartup() {
        log.warn("【投票同步任务】项目启动，执行一次强制同步");
        syncVoteCountsFromRedisToDatabase();
    }

    private void syncCounts() {
        List<Song> allSongs = songRepository.findAll();
        if (allSongs.isEmpty()) {
            return;
        }

        List<Object> voteCounts = fetchVoteCountsWithPipeline(allSongs);
        List<Song> changedSongs = new ArrayList<>();
        int resultIndex = 0;

        for (Song song : allSongs) {
            if (song.getId() == null) {
                continue;
            }
            int newLikes = readPipelineCount(voteCounts, resultIndex++);
            int newDislikes = readPipelineCount(voteCounts, resultIndex++);
            if (song.getLikeCount() != newLikes || song.getDislikeCount() != newDislikes) {
                song.setLikeCount(newLikes);
                song.setDislikeCount(newDislikes);
                changedSongs.add(song);
            }
        }

        if (!changedSongs.isEmpty()) {
            songRepository.saveAll(changedSongs);
        }
        log.info("【投票同步任务】完成！本次更新 {} 首歌曲的票数", changedSongs.size());
    }

    private void rebuildRedisVotes() {
        deleteVoteKeys("likes:*");
        deleteVoteKeys("dislikes:*");
        List<SongVote> votes = songVoteRepository.findAll();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            for (SongVote vote : votes) {
                if (vote.getSongId() == null || vote.getUserId() == null) {
                    continue;
                }
                String key = (vote.getVoteType().name().equals("LIKE") ? "likes:" : "dislikes:") + vote.getSongId();
                connection.setCommands().sAdd(serializer.serialize(key), serializer.serialize(vote.getUserId().toString()));
            }
            return null;
        });
        redisTemplate.opsForValue().set(REBUILD_MARKER, "true");
        log.info("【投票同步任务】已从 MySQL 重建 {} 条 Redis 投票关系", votes.size());
    }

    private void deleteVoteKeys(String pattern) {
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new java.util.HashSet<>();
            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            try (var cursor = connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                    .match(pattern).count(500).build())) {
                while (cursor.hasNext()) {
                    result.add(serializer.deserialize(cursor.next()));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to scan Redis vote keys", e);
            }
            return result;
        });
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private List<Object> fetchVoteCountsWithPipeline(List<Song> songs) {
        return redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            for (Song song : songs) {
                Long songId = song.getId();
                if (songId == null) {
                    continue;
                }
                connection.setCommands().sCard(serializer.serialize("likes:" + songId));
                connection.setCommands().sCard(serializer.serialize("dislikes:" + songId));
            }
            return null;
        });
    }

    private int readPipelineCount(List<Object> voteCounts, int index) {
        if (index >= voteCounts.size()) {
            return 0;
        }
        Object value = voteCounts.get(index);
        return value instanceof Number number ? number.intValue() : 0;
    }
}