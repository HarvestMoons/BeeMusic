package com.example.musicplayer.task;
import com.example.musicplayer.model.Song;
import com.example.musicplayer.repository.SongRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoteCountSyncTask {

    private final SongRepository songRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 每5分钟把 Redis 实时票数回写到数据库 songs.like_count / dislike_count
     * 加上这个任务后：就算 Redis 整个挂掉/重启/清空，票数也永远不会丢
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)  // 每5分钟执行一次
    // 也可以用 cron： @Scheduled(cron = "0 */5 * * * ?")
    public void syncVoteCountsFromRedisToDatabase() {
        log.info("【投票同步任务】开始执行 Redis → MySQL 票数回写");

        // 1. 查出所有有歌曲（实际项目可能几千首，这里一次性全量同步，成本很低）
        List<Song> allSongs = songRepository.findAll();

        if (allSongs.isEmpty()) {
            log.info("暂无歌曲，跳过本次同步");
            return;
        }

        int updatedCount = 0;

        for (Song song : allSongs) {
            Long songId = song.getId();
            if (songId == null) {
                continue;
            }

            String likesKey = "likes:" + songId;
            String dislikesKey = "dislikes:" + songId;

            Long redisLikes = redisTemplate.opsForSet().size(likesKey);
            Long redisDislikes = redisTemplate.opsForSet().size(dislikesKey);

            int newLikes = redisLikes != null ? redisLikes.intValue() : 0;
            int newDislikes = redisDislikes != null ? redisDislikes.intValue() : 0;

            // 只有数字不一致才更新（避免无意义的写库）
            if (song.getLikeCount() != newLikes || song.getDislikeCount() != newDislikes) {
                song.setLikeCount(newLikes);
                song.setDislikeCount(newDislikes);
                updatedCount++;
            }
        }

        // 2. 批量保存（性能极高，几千首歌也就几毫秒）
        if (updatedCount > 0) {
            songRepository.saveAll(allSongs);
            log.info("【投票同步任务】完成！本次更新 {} 首歌曲的票数", updatedCount);
        } else {
            log.info("【投票同步任务】完成！本次无变化，全部已是最新");
        }
    }

    /**
     * 项目启动时强制同步一次（防止 Redis 冷启动后票数为0）
     */
    @PostConstruct   // 启动时也同步一次
    public void syncOnStartup() {
        log.warn("【投票同步任务】项目启动，执行一次强制同步");
        syncVoteCountsFromRedisToDatabase();
    }
}