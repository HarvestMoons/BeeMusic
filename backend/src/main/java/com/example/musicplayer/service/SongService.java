package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.example.musicplayer.model.Song;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.aliyun.oss.model.ListObjectsRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class SongService {

    private final StringRedisTemplate redisTemplate;
    private final OSS ossClient;
    private final String bucketName = "bees-bucket";

    private final Map<String, String> AVAILABLE_FOLDERS = Map.of(
            "ha_ji_mi", "哈基米",
            "dian_gun", "溜冰场",
            "da_si_ma", "大司马",
            "ding_zhen", "丁真",
            "dxl", "东洋雪莲"
    );

    private String currentFolderKey = "ha_ji_mi";

    public SongService(
            StringRedisTemplate redisTemplate,
            @Value("${aliyun.oss.endpoint}") String endpoint,
            @Value("${aliyun.oss.access-key}") String accessKey,
            @Value("${aliyun.oss.secret-key}") String secretKey) {
        this.redisTemplate = redisTemplate;
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
    }

    // 获取歌曲列表（不限量）
    public List<Song> getSongs() {
        String folder = AVAILABLE_FOLDERS.get(currentFolderKey);
        String prefix = "music/" + folder + "/";

        List<Song> songs = new ArrayList<>();
        String nextMarker = null;
        ObjectListing objectListing;

        do {
            objectListing = ossClient.listObjects(new ListObjectsRequest(bucketName)
                    .withPrefix(prefix)
                    .withMarker(nextMarker)
                    // 每次最多 1000 条
                    .withMaxKeys(1000));

            for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {
                String key = summary.getKey();
                if (key.toLowerCase().endsWith(".mp3")) {
                    String filename = key.substring(key.lastIndexOf("/") + 1);
                    String signedUrl = ossClient.generatePresignedUrl(
                            bucketName, key,
                            new Date(System.currentTimeMillis() + 60 * 60 * 1000 * 24)
                    ).toString();

                    Song song = new Song();
                    song.setId(Integer.toHexString(key.hashCode()));
                    song.setName(filename);
                    song.setUrl(signedUrl);
                    song.setKey(key);

                    songs.add(song);
                }
            }
            nextMarker = objectListing.getNextMarker();
        } while (objectListing.isTruncated()); // 若结果被截断则继续请求

        return songs;
    }


    // 切换文件夹
    public Map<String, Object> setFolder(String folder) {
        if (AVAILABLE_FOLDERS.containsKey(folder)) {
            currentFolderKey = folder;
            return Map.of("status", "ok", "current", folder);
        }
        return Map.of("error", "Invalid folder key");
    }

    // 获取点赞/点踩数
    public Map<String, Integer> getVotes(String songId) {
        String likes = redisTemplate.opsForValue().get("likes:" + songId);
        String dislikes = redisTemplate.opsForValue().get("dislikes:" + songId);

        return Map.of(
                "likes", likes != null ? Integer.parseInt(likes) : 0,
                "dislikes", dislikes != null ? Integer.parseInt(dislikes) : 0
        );
    }

    // 点赞
    public Map<String, Integer> likeSong(String songId) {
        redisTemplate.opsForValue().increment("likes:" + songId);
        return getVotes(songId);
    }

    // 点踩
    public Map<String, Integer> dislikeSong(String songId) {
        redisTemplate.opsForValue().increment("dislikes:" + songId);
        return getVotes(songId);
    }

}
