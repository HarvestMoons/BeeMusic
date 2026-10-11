package com.example.musicplayer.service;

import com.aliyun.oss.model.OSSObjectSummary;
import com.example.musicplayer.dto.FolderSongCount;
import com.example.musicplayer.model.Song;
import com.example.musicplayer.repository.CommentLikeRepository;
import com.example.musicplayer.repository.CommentRepository;
import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.repository.SongVoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SongService {

    private static final String DEFAULT_FOLDER_KEY = "ha_ji_mi";

    private final OssUtil ossUtil;
    private final SongRepository songRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final SongVoteRepository songVoteRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;

    private final Map<String, String> AVAILABLE_FOLDERS = Map.of(
            "ha_ji_mi", "哈基米",
            "dian_gun", "溜冰场",
            "da_si_ma", "大司马",
            "ding_zhen", "丁真",
            "dxl", "东洋雪莲",
            "DDF", "哲学",
            "true_music", "真正的音乐");

    public SongService(OssUtil ossUtil, SongRepository songRepository,
            CommentRepository commentRepository,
            CommentLikeRepository commentLikeRepository,
            SongVoteRepository songVoteRepository,
            RedisTemplate<String, Object> jsonRedisTemplate,
            org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate,
            PlatformTransactionManager transactionManager) {
        this.ossUtil = ossUtil;
        this.songRepository = songRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.songVoteRepository = songVoteRepository;
        this.redisTemplate = jsonRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void incrementPlayCount(Long songId) {
        songRepository.incrementPlayCount(songId);
    }

    public List<FolderSongCount> getFolderSongCounts() {
        return AVAILABLE_FOLDERS.entrySet().stream()
                .map(entry -> {
                    String prefix = "music/" + entry.getValue() + "/";
                    long count = songRepository.countActiveSongsByPrefix(prefix);
                    return new FolderSongCount(entry.getKey(), entry.getValue(), count);
                })
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<Song> getSongs(String folderKey, boolean includeDeleted) {
        String normalizedFolderKey = normalizeFolderKey(folderKey);
        String folderChineseName = getFolderChineseName(normalizedFolderKey);
        String prefix = "music/" + folderChineseName + "/";
        String cacheKey = "songs:folder:" + normalizedFolderKey + (includeDeleted ? ":all" : ":active");

        // 1. 查缓存
        List<Song> cachedSongs = (List<Song>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedSongs != null) {
            // 缓存命中，虽然缓存里有 Song 数据，但缓存里的点赞数可能是旧的(即使缓存只存6小时)
            // 所以我们需要遍历缓存里的 Song，用 Redis 里的最新票数覆盖一下
            hydrateSongVoteCounts(cachedSongs);
            return cachedSongs;
        }

        // 2. 缓存未命中，查数据库
        List<Song> dbSongs = songRepository.findByKeyStartingWith(prefix);

        if (!includeDeleted) {
            dbSongs = dbSongs.stream()
                    .filter(s -> s.getIsDeleted() == 0)
                    .collect(Collectors.toList());
        }

        for (Song song : dbSongs) {
            song.setUrl(ossUtil.buildSignedUrl(song.getKey()));
        }

        // 4. 实时修正票数：用 Redis 里的最新票数覆盖 DB/缓存里的旧数据
        hydrateSongVoteCounts(dbSongs);

        // 5. 写入缓存 (设置过期时间为 6 小时，远小于 OSS 签名的 24 小时，确保取出的 URL 总是有效的)
        if (!dbSongs.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, dbSongs, 6, TimeUnit.HOURS);
        }

        return dbSongs;
    }

    public void deleteSong(Long songId) {
        Optional<Song> songOpt = songRepository.findById(songId);
        if (songOpt.isPresent()) {
            Song song = songOpt.get();
            song.setIsDeleted(1);
            songRepository.save(song);
            evictAllFolderCaches();
        }
    }

    public void restoreSong(Long songId) {
        Optional<Song> songOpt = songRepository.findById(songId);
        if (songOpt.isPresent()) {
            Song song = songOpt.get();
            song.setIsDeleted(0);
            songRepository.save(song);
            evictAllFolderCaches();
        }
    }

    private void evictAllFolderCaches() {
        Set<String> keys = redisTemplate
                .execute((org.springframework.data.redis.core.RedisCallback<Set<String>>) connection -> {
                    Set<String> result = new HashSet<>();
                    org.springframework.data.redis.serializer.RedisSerializer<String> serializer = redisTemplate
                            .getStringSerializer();
                    try (var cursor = connection.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match("songs:folder:*").count(500).build())) {
                        while (cursor.hasNext()) {
                            result.add(serializer.deserialize(cursor.next()));
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to scan song cache keys", e);
                    }
                    return result;
                });
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 只新增的同步。用于定时任务和启动时同步，永远不会删除数据。
     */
    public SyncResult syncAllSongs() {
        return syncAllSongs(false);
    }

    /**
     * 站长手动同步：先同步新增，再清理 OSS 中已不存在的歌曲（硬删除）。
     * 该方法会永久删除歌曲及其投票、评论、评论点赞，必须只在人工确认后调用。
     */
    public SyncResult syncAllSongsAndCleanupOrphans() {
        return syncAllSongs(true);
    }

    private SyncResult syncAllSongs(boolean cleanupOrphans) {
        int added = 0;
        int removed = 0;
        try {
            for (Map.Entry<String, String> folder : AVAILABLE_FOLDERS.entrySet()) {
                String folderKey = folder.getKey();
                String prefix = "music/" + folder.getValue() + "/";
                // 每个文件夹只列举一次 OSS，新增与清理复用同一份结果
                List<OSSObjectSummary> ossObjects = ossUtil.listFilesByPrefix(prefix);
                added += addNewSongs(folderKey, prefix, ossObjects);
                if (cleanupOrphans) {
                    removed += removeOrphanSongs(folderKey, prefix, ossObjects);
                }
            }
        } finally {
            evictAllFolderCaches();
        }
        return new SyncResult(added, removed);
    }

    /**
     * 把 OSS 中存在、数据库中还没有的 mp3 写入数据库（只新增，不删除任何记录）。
     *
     * @param ossObjects 该文件夹下 OSS 的全部对象，本方法内部按 .mp3 过滤
     * @return 新增的歌曲数量
     */
    private int addNewSongs(String folderKey, String prefix, List<OSSObjectSummary> ossObjects) {
        Set<String> dbKeys = songRepository.findByKeyStartingWith(prefix).stream()
                .map(Song::getKey)
                .collect(Collectors.toSet());

        List<Song> newSongs = new ArrayList<>();
        for (OSSObjectSummary file : ossObjects) {
            String key = file.getKey();
            if (!key.toLowerCase(Locale.ROOT).endsWith(".mp3") || dbKeys.contains(key)) {
                continue;
            }
            Song song = new Song();
            song.setName(ossUtil.extractFilename(key));
            song.setKey(key);
            song.setLikeCount(0);
            song.setDislikeCount(0);
            song.setPlayCount(0);
            song.setIsDeleted(0);
            newSongs.add(song);
        }

        if (!newSongs.isEmpty()) {
            songRepository.saveAll(newSongs);
            // 只有当有新歌时才清除缓存
            redisTemplate.delete("songs:folder:" + folderKey + ":all");
            redisTemplate.delete("songs:folder:" + folderKey + ":active");
        }
        return newSongs.size();
    }

    /**
     * 删除数据库中「OSS 里已经不存在」的歌曲记录。
     * 只在该文件夹 OSS 列表非空时才清理。
     *
     * @param ossObjects 本次同步已列举到的该文件夹 OSS 对象，复用避免重复列举
     * @return 被删除的歌曲数量
     */
    private int removeOrphanSongs(String folderKey, String prefix, List<OSSObjectSummary> ossObjects) {
        List<Song> dbSongs = songRepository.findByKeyStartingWith(prefix);
        if (dbSongs.isEmpty()) {
            return 0;
        }

        Set<String> ossKeys = ossObjects.stream()
                .map(OSSObjectSummary::getKey)
                .collect(Collectors.toSet());

        // 安全阀：OSS 返回空列表通常意味着网络/权限/配置异常，此时绝不能把整个文件夹的记录删掉
        if (ossKeys.isEmpty()) {
            log.error("【歌曲清理】文件夹 {} 的 OSS 列表为空但数据库有 {} 条记录，疑似 OSS 异常，已跳过清理",
                    folderKey, dbSongs.size());
            return 0;
        }

        List<Song> orphans = dbSongs.stream()
                .filter(song -> song.getKey() != null && !ossKeys.contains(song.getKey()))
                .toList();
        if (orphans.isEmpty()) {
            return 0;
        }

        int removed = 0;
        for (Song orphan : orphans) {
            try {
                deleteSongHard(orphan);
                removed++;
            } catch (RuntimeException e) {
                log.error("【歌曲清理】删除歌曲失败，已跳过: id={}, objectKey={}", orphan.getId(), orphan.getKey(), e);
            }
        }
        log.warn("【歌曲清理】文件夹 {} 清理了 {} 首 OSS 中已不存在的歌曲: {}", folderKey, removed,
                orphans.stream().map(Song::getKey).collect(Collectors.toList()));
        return removed;
    }

    /**
     * 硬删除歌曲记录及其全部关联数据。
     * 实测数据库：song_votes.song_id 带 ON DELETE CASCADE，但 comments / comment_likes
     * 没有任何外键，
     * 不显式清理会留下孤儿数据，所以这里按 comment_likes -> comments -> song_votes -> songs 的顺序删除。
     */
    private void deleteSongHard(Song song) {
        Long songId = song.getId();
        if (songId == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            commentLikeRepository.deleteBySongId(songId);
            commentRepository.deleteBySongId(songId);
            songVoteRepository.deleteBySongId(songId);
            songRepository.deleteById(songId);
        });
        // 事务提交后再清理 Redis 实时投票读模型，避免删除失败时 Redis 已被清空
        stringRedisTemplate.delete(List.of("likes:" + songId, "dislikes:" + songId));
        log.info("【歌曲清理】已删除歌曲: id={}, name={}, objectKey={}", songId, song.getName(), song.getKey());
    }

    /**
     * 同步结果统计
     *
     * @param added   本次新增的歌曲数
     * @param removed 本次清理（硬删除）的歌曲数
     */
    public record SyncResult(int added, int removed) {
    }

    private void hydrateSongVoteCounts(List<Song> songs) {
        if (songs.isEmpty()) {
            return;
        }
        List<Object> counts = stringRedisTemplate
                .executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    org.springframework.data.redis.serializer.RedisSerializer<String> serializer = stringRedisTemplate
                            .getStringSerializer();
                    for (Song song : songs) {
                        if (song.getId() != null) {
                            connection.setCommands().sCard(serializer.serialize("likes:" + song.getId()));
                            connection.setCommands().sCard(serializer.serialize("dislikes:" + song.getId()));
                        }
                    }
                    return null;
                });
        int index = 0;
        for (Song song : songs) {
            if (song.getId() == null) {
                continue;
            }
            song.setLikeCount(readCount(counts, index++));
            song.setDislikeCount(readCount(counts, index++));
        }
    }

    private int readCount(List<Object> counts, int index) {
        if (index >= counts.size() || !(counts.get(index) instanceof Number number)) {
            return 0;
        }
        return number.intValue();
    }

    private String normalizeFolderKey(String folderKey) {
        if (folderKey == null || folderKey.isBlank()) {
            return DEFAULT_FOLDER_KEY;
        }
        return folderKey;
    }

    private String getFolderChineseName(String folderKey) {
        String folderChineseName = AVAILABLE_FOLDERS.get(folderKey);
        if (folderChineseName == null) {
            throw new IllegalArgumentException("Invalid folder key: " + folderKey);
        }
        return folderChineseName;
    }
}
