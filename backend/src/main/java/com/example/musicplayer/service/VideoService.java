package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObjectSummary;
import com.example.musicplayer.model.Video;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final OSS ossClient;
    private final OssUtil ossUtil;

    @Value("${aliyun.oss.bucket-name:bees-bucket}")
    private String bucketName;

    private static final String VIDEO_PREFIX = "videos/";
    // 签名链接有效期 24 小时
    private static final long URL_EXPIRE_HOURS = 24;

    // 构造函数注入（推荐写法）
    public VideoService(OSS ossClient, OssUtil ossUtil) {
        this.ossClient = ossClient;
        this.ossUtil = ossUtil;
    }

    /**
     * 获取所有视频（自动分页 + 复用 OssUtil 逻辑 + 生成签名 URL）
     */
    public List<Video> getVideos() {
        // 复用 OssUtil 的分页逻辑，只改了个后缀判断
        List<OSSObjectSummary> videoSummaries = ossUtil.listFilesByPrefixAndSuffix(VIDEO_PREFIX, ".mp4");

        return videoSummaries.stream()
                .map(summary -> {
                    String key = summary.getKey();
                    String filename = key.substring(key.lastIndexOf('/') + 1);

                    String signedUrl = ossClient.generatePresignedUrl(
                            bucketName,
                            key,
                            new Date(System.currentTimeMillis() + URL_EXPIRE_HOURS * 60 * 60 * 1000)
                    ).toString();

                    Video video = new Video();
                    // 用 key 的 hash 作为临时 id（保证唯一，且不依赖数据库）
                    video.setId(String.valueOf(key.hashCode()));
                    video.setName(filename);
                    video.setUrl(signedUrl);
                    video.setKey(key);
                    return video;
                })
                .collect(Collectors.toList());
    }
}