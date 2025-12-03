package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObjectSummary;
import com.example.musicplayer.model.Song;
import com.example.musicplayer.repository.SongRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SongService {

    private final OSS ossClient;
    private final OssUtil ossUtil;
    private final SongRepository songRepository;
    private final Map<String, String> AVAILABLE_FOLDERS = Map.of(
            "ha_ji_mi", "哈基米",
            "dian_gun", "溜冰场",
            "da_si_ma", "大司马",
            "ding_zhen", "丁真",
            "dxl", "东洋雪莲",
            "DDF","哲学"
    );

    private String currentFolderKey = "ha_ji_mi";

    public SongService(OSS ossClient, OssUtil ossUtil, SongRepository songRepository) {
        this.ossClient = ossClient;
        this.ossUtil = ossUtil;
        this.songRepository = songRepository;
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

    public List<Song> getSongs() {
        String folderChineseName = AVAILABLE_FOLDERS.get(currentFolderKey);
        String prefix = "music/" + folderChineseName + "/";

        // 第一步：先从 OSS 列出当前文件夹所有 mp3 文件的 key
        List<OSSObjectSummary> ossFiles = ossUtil.listFilesByPrefixAndSuffix(prefix, ".mp3");

        // 第二步：从数据库查出当前文件夹所有已持久化的歌曲（按 key 去重）
        List<Song> dbSongs = songRepository.findByKeyStartingWithAndIsDeleted(prefix, 0);

        // 第三步：生成 signedUrl 并合并（关键！要复用数据库里的 id 和投票数）
        Map<String, Song> keyToSongMap = new HashMap<>();
        for (Song song : dbSongs) {
            keyToSongMap.put(song.getKey(), song);
        }

        List<Song> result = new ArrayList<>();
        for (OSSObjectSummary file : ossFiles) {
            String key = file.getKey();

            // 优先复用数据库已有的记录（包含 id、投票数、创建时间等）
            Song song = keyToSongMap.getOrDefault(key, new Song());

            // 如果是新文件（数据库没有），则补全基本信息
            if (song.getId() == null) {
                String filename = key.substring(key.lastIndexOf('/') + 1);
                song.setName(filename);
                song.setKey(key);
                song.setLikeCount(0);
                song.setDislikeCount(0);
                song.setPlayCount(0);
                song.setIsDeleted(0);
                // 注意：这里不要手动 setId，让数据库稍后插入时自动生成
            }

            // 生成临时签名 URL（24小时有效）
            String signedUrl = ossClient.generatePresignedUrl(
                    "bees-bucket",
                    key,
                    new Date(System.currentTimeMillis() + 24 * 3600 * 1000)
            ).toString();

            song.setUrl(signedUrl);
            result.add(song);
        }
        // 自动保存新歌（幂等操作，没性能问题）
        songRepository.saveAll(result.stream()
                .filter(s -> s.getId() == null)
                .collect(Collectors.toList()));
        return result;
    }
}

