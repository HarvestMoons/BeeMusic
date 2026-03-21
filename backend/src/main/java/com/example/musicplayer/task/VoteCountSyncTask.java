package com.example.musicplayer.task;

import com.example.musicplayer.model.Song;
import com.example.musicplayer.repository.SongRepository;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteCountSyncTask {

    private final SongRepository songRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 定时把 Redis 实时票数回写到数据库 songs.like_count / dislike_count
     * 加上这个任务后：就算 Redis 整个挂掉/重启/清空，票数也永远不会丢
     */
    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.MINUTES) // 每60分钟执行一次
    // 也可以用 cron： @Scheduled(cron = "0 */5 * * * ?")
    public void syncVoteCountsFromRedisToDatabase() {
        log.info("【投票同步任务】开始执行 Redis → MySQL 票数回写");

        // 1. 查出所有有歌曲（实际项目可能几千首，这里一次性全量同步，成本很低）
        List<Song> allSongs = songRepository.findAll();

        if (allSongs.isEmpty()) {
            return;
        }

        List<Object> voteCounts = fetchVoteCountsWithPipeline(allSongs);
        List<Song> changedSongs = new ArrayList<>();
        int updatedCount = 0;
        int resultIndex = 0;

        for (Song song : allSongs) {
            Long songId = song.getId();
            if (songId == null) {
                continue;
            }

            int newLikes = readPipelineCount(voteCounts, resultIndex++);
            int newDislikes = readPipelineCount(voteCounts, resultIndex++);

            // 只有数字不一致才更新（避免无意义的写库）
            if (song.getLikeCount() != newLikes || song.getDislikeCount() != newDislikes) {
                song.setLikeCount(newLikes);
                song.setDislikeCount(newDislikes);
                changedSongs.add(song);
                updatedCount++;
            }
        }

        // 2. 批量保存（性能极高，几千首歌也就几毫秒）
        if (updatedCount > 0) {
            songRepository.saveAll(changedSongs);
            log.info("【投票同步任务】完成！本次更新 {} 首歌曲的票数", updatedCount);
        } else {
            log.info("【投票同步任务】完成！本次无变化，全部已是最新");
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