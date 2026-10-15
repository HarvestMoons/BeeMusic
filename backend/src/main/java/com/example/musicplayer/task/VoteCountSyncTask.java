package com.example.musicplayer.task;

import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.redis.RedisTaskLock;
import com.example.musicplayer.service.VoteRedisProjector;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteCountSyncTask {
    private final SongRepository songRepository;
    private final VoteRedisProjector redisProjector;
    private final RedisTaskLock taskLock;
    private static final String LOCK_KEY = "music:task-lock:vote-projection";

    @Scheduled(fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void rebuildRedisFromDatabase() {
        String token = taskLock.tryAcquire(LOCK_KEY, Duration.ofHours(2));
        if (token == null) return;
        try {
            int rebuilt = 0;
            for (Long songId : songRepository.findAllIds()) {
                redisProjector.rebuild(songId);
                rebuilt++;
            }
            log.info("【投票同步任务】已逐首原子重建 {} 首歌曲的投票", rebuilt);
        } finally {
            taskLock.release(LOCK_KEY, token);
        }
    }

    @PostConstruct
    public void syncOnStartup() {
        rebuildRedisFromDatabase();
    }
}
