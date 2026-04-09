package com.example.musicplayer.service;

import com.aliyun.oss.model.OSSObjectSummary;
import com.example.musicplayer.dto.FolderSongCount;
import com.example.musicplayer.model.Song;
import com.example.musicplayer.repository.SongRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SongService {

    private static final String DEFAULT_FOLDER_KEY = "ha_ji_mi";

    private final OssUtil ossUtil;
    private final SongRepository songRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private final Map<String, String> AVAILABLE_FOLDERS = Map.of(
            "ha_ji_mi", "哈基米",
            "dian_gun", "溜冰场",
            "da_si_ma", "大司马",
            "ding_zhen", "丁真",
            "dxl", "东洋雪莲",
            "DDF", "哲学",
            "true_music", "真正的音乐");

    public SongService(OssUtil ossUtil, SongRepository songRepository,
            RedisTemplate<String, Object> jsonRedisTemplate,
            org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate) {
        this.ossUtil = ossUtil;
        this.songRepository = songRepository;
        this.redisTemplate = jsonRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
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
                String filename = ossUtil.extractFilename(key);
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

    public void syncAllSongs() {
        for (String folderKey : AVAILABLE_FOLDERS.keySet()) {
            syncSongsFromOss(folderKey);
        }
    }

    private void hydrateSongVoteCounts(List<Song> songs) {
        for (Song song : songs) {
            Long songId = song.getId();
            if (songId != null) {
                Long redisLikes = stringRedisTemplate.opsForSet().size("likes:" + songId);
                Long redisDislikes = stringRedisTemplate.opsForSet().size("dislikes:" + songId);
                song.setLikeCount(redisLikes != null ? redisLikes.intValue() : 0);
                song.setDislikeCount(redisDislikes != null ? redisDislikes.intValue() : 0);
            }
        }
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
