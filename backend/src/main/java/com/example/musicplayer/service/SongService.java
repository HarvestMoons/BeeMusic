package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObjectSummary;
import com.example.musicplayer.dto.FolderSongCount;
import com.example.musicplayer.model.Song;
import com.example.musicplayer.repository.SongRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SongService {

    private final OSS ossClient;
    private final OssUtil ossUtil;
    private final SongRepository songRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Map<String, String> AVAILABLE_FOLDERS = Map.of(
            "ha_ji_mi", "哈基米",
            "dian_gun", "溜冰场",
            "da_si_ma", "大司马",
            "ding_zhen", "丁真",
            "dxl", "东洋雪莲",
            "DDF", "哲学",
            "true_music", "真正的音乐"
    );

    private String currentFolderKey = "ha_ji_mi";

    public SongService(OSS ossClient, OssUtil ossUtil, SongRepository songRepository, RedisTemplate<String, Object> jsonRedisTemplate) {
        this.ossClient = ossClient;
        this.ossUtil = ossUtil;
        this.songRepository = songRepository;
        this.redisTemplate = jsonRedisTemplate;
    }

    public Map<String, String> setFolder(String folder) {
        if (AVAILABLE_FOLDERS.containsKey(folder)) {
            currentFolderKey = folder;
            return Map.of("status", "ok", "current", folder);
        }
        return Map.of("error", "Invalid folder key");
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
    public List<Song> getSongs(boolean includeDeleted) {
        long start = System.currentTimeMillis();
        String folderChineseName = AVAILABLE_FOLDERS.get(currentFolderKey);
        String prefix = "music/" + folderChineseName + "/";
        String cacheKey = "songs:folder:" + currentFolderKey + (includeDeleted ? ":all" : ":active");

        // 1. 查缓存
        List<Song> cachedSongs = (List<Song>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedSongs != null) {
            log.info("Cache hit for key: {}", cacheKey);
            // 缓存命中，重新生成签名 URL (因为 URL 有有效期，缓存可能存活久，但 URL 过期)
            for (Song song : cachedSongs) {
                String signedUrl = ossClient.generatePresignedUrl(
                        ossUtil.getBucketName(),
                        song.getKey(),
                        ossUtil.getExpirationDate()
                ).toString();
                song.setUrl(signedUrl);
            }
            log.info("Served from cache in {}ms", System.currentTimeMillis() - start);
            return cachedSongs;
        }
        log.info("Cache miss for key: {}", cacheKey);

        // 2. 缓存未命中，查数据库 (不再同步调用 OSS sync)
        List<Song> dbSongs = songRepository.findByKeyStartingWith(prefix);
        
        if (!includeDeleted) {
            dbSongs = dbSongs.stream()
                    .filter(s -> s.getIsDeleted() == 0)
                    .collect(Collectors.toList());
        }

        // 3. 写入缓存 (10分钟)
        redisTemplate.opsForValue().set(cacheKey, dbSongs, 10, TimeUnit.MINUTES);
        
        // 生成 signedUrl
        for (Song song : dbSongs) {
            String signedUrl = ossClient.generatePresignedUrl(
                    ossUtil.getBucketName(),
                    song.getKey(),
                    ossUtil.getExpirationDate()
            ).toString();
            song.setUrl(signedUrl);
        }
        log.info("Served from DB in {}ms", System.currentTimeMillis() - start);
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
        Set<String> keys = redisTemplate.keys("songs:folder:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }


    /**
     * 同步指定文件夹的 OSS 文件到数据库
     */
    public void syncSongsFromOss(String folderKey) {
        String folderChineseName = AVAILABLE_FOLDERS.get(folderKey);
        if (folderChineseName == null) {
            return;
        }
        String prefix = "music/" + folderChineseName + "/";

        // 1. OSS 列出文件
        List<OSSObjectSummary> ossFiles = ossUtil.listFilesByPrefixAndSuffix(prefix, ".mp3");

        // 2. 数据库查出已有文件
        List<Song> dbSongs = songRepository.findByKeyStartingWith(prefix);
        Set<String> dbKeys = dbSongs.stream().map(Song::getKey).collect(Collectors.toSet());

        // 3. 找出新文件
        List<Song> newSongs = new ArrayList<>();
        for (OSSObjectSummary file : ossFiles) {
            if (!dbKeys.contains(file.getKey())) {
                Song song = new Song();
                String key = file.getKey();
                String filename = key.substring(key.lastIndexOf('/') + 1);
                song.setName(filename);
                song.setKey(key);
                song.setLikeCount(0);
                song.setDislikeCount(0);
                song.setPlayCount(0);
                song.setIsDeleted(0);
                newSongs.add(song);
            }
        }

        // 4. 保存新文件
        if (!newSongs.isEmpty()) {
            songRepository.saveAll(newSongs);
            // 只有当有新歌时才清除缓存
            redisTemplate.delete("songs:folder:" + folderKey + ":all");
            redisTemplate.delete("songs:folder:" + folderKey + ":active");
        }
    }

    /**
     * 同步所有文件夹 (定时任务：每10分钟一次)
     */
    @Scheduled(fixedRate = 600000)
    public void syncAllSongs() {
        for (String key : AVAILABLE_FOLDERS.keySet()) {
            syncSongsFromOss(key);
        }
    }
}

