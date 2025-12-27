package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObjectSummary;
import com.example.musicplayer.dto.FolderSongCount;
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
            "DDF","哲学",
            "true_music", "真正的音乐"
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

    public List<FolderSongCount> getFolderSongCounts() {
        return AVAILABLE_FOLDERS.entrySet().stream()
                .map(entry -> {
                    String prefix = "music/" + entry.getValue() + "/";
                    long count = songRepository.countActiveSongsByPrefix(prefix);
                    return new FolderSongCount(entry.getKey(), entry.getValue(), count);
                })
                .collect(Collectors.toList());
    }

    public List<Song> getSongs() {
        String folderChineseName = AVAILABLE_FOLDERS.get(currentFolderKey);
        String prefix = "music/" + folderChineseName + "/";

        // 同步该文件夹的歌曲元数据
        syncSongsFromOss(currentFolderKey);

        // 从数据库查出当前文件夹所有已持久化的歌曲
        List<Song> dbSongs = songRepository.findByKeyStartingWithAndIsDeleted(prefix, 0);

        // 生成 signedUrl
        for (Song song : dbSongs) {
            String signedUrl = ossClient.generatePresignedUrl(
                    "bees-bucket",
                    song.getKey(),
                    new Date(System.currentTimeMillis() + 24 * 3600 * 1000)
            ).toString();
            song.setUrl(signedUrl);
        }
        return dbSongs;
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
        Set<String> ossKeys = ossFiles.stream().map(OSSObjectSummary::getKey).collect(Collectors.toSet());

        // 2. 数据库查出已有文件
        List<Song> dbSongs = songRepository.findByKeyStartingWithAndIsDeleted(prefix, 0);
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
        }
    }

    /**
     * 同步所有文件夹
     */
    public void syncAllSongs() {
        for (String key : AVAILABLE_FOLDERS.keySet()) {
            syncSongsFromOss(key);
        }
    }
}

