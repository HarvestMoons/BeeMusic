package com.example.musicplayer.task;

import com.example.musicplayer.service.SongService;
import com.example.musicplayer.service.RedisTaskLock;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongSyncTask {

    private final SongService songService;
    private final RedisTaskLock taskLock;

    private static final String LOCK_KEY = "music:task-lock:song-sync";

    /**
     * 每30分钟同步一次 OSS 歌曲元数据到数据库
     * 确保新上传的歌曲能被及时发现
     */
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void syncSongs() {
        String token = taskLock.tryAcquire(LOCK_KEY, Duration.ofHours(2));
        if (token == null) {
            log.info("【歌曲同步任务】已有其他实例执行，跳过本次任务");
            return;
        }
        log.info("【歌曲同步任务】开始执行 OSS → MySQL 歌曲元数据同步");
        try {
            songService.syncAllSongs();
            log.info("【歌曲同步任务】完成");
        } catch (Exception e) {
            log.error("【歌曲同步任务】执行失败", e);
        } finally {
            taskLock.release(LOCK_KEY, token);
        }
    }

    /**
     * 项目启动时执行一次同步
     */
    @PostConstruct
    public void syncOnStartup() {
        log.info("【歌曲同步任务】项目启动，执行一次全量同步");
        // 使用新线程执行，避免阻塞主线程启动
        new Thread(() -> {
            try {
                // 稍微延迟一点，避免影响启动速度
                Thread.sleep(5000);
                syncSongs();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
