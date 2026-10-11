package com.example.musicplayer.task;

import com.example.musicplayer.model.SongVote;
import com.example.musicplayer.repository.SongVoteRepository;
import com.example.musicplayer.service.RedisKeyScanner;
import com.example.musicplayer.service.RedisTaskLock;
import com.example.musicplayer.service.VoteRedisKeys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteCountSyncTask {

    private final SongVoteRepository songVoteRepository;
    private final StringRedisTemplate redisTemplate;
    private final RedisTaskLock taskLock;

    private final RedisKeyScanner redisKeyScanner;
    private static final String LOCK_KEY = "music:task-lock:vote-projection";
    /**
     * 定期从 MySQL 事实表重建 Redis 读模型。
     */
    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.MINUTES) // 每60分钟执行一次
    public void rebuildRedisFromDatabase() {
        String token = taskLock.tryAcquire(LOCK_KEY, Duration.ofHours(2));
        if (token == null) {
            log.info("【投票同步任务】已有其他实例执行，跳过本次任务");
            return;
        }
        log.info("【投票同步任务】开始从 MySQL 重建 Redis 投票读模型");
        try {
            rebuildRedisVotes();
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
        rebuildRedisFromDatabase();
    }

    private void rebuildRedisVotes() {
        redisKeyScanner.deleteByPattern(VoteRedisKeys.LIKES_PATTERN);
        redisKeyScanner.deleteByPattern(VoteRedisKeys.DISLIKES_PATTERN);
        List<SongVote> votes = songVoteRepository.findAll();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            for (SongVote vote : votes) {
                if (vote.getSongId() == null || vote.getUserId() == null) {
                    continue;
                }
                String key = VoteRedisKeys.byType(vote.getSongId(), vote.getVoteType());
                connection.setCommands().sAdd(serializer.serialize(key), serializer.serialize(vote.getUserId().toString()));
            }
            return null;
        });
        log.info("【投票同步任务】已从 MySQL 重建 {} 条 Redis 投票关系", votes.size());
    }

}