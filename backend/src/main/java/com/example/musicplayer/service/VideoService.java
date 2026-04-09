package com.example.musicplayer.service;

import com.aliyun.oss.model.OSSObjectSummary;
import com.example.musicplayer.model.Video;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final OssUtil ossUtil;

    private static final String VIDEO_PREFIX = "videos/";

    // 构造函数注入（推荐写法）
    public VideoService(OssUtil ossUtil) {
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
                    String filename = ossUtil.extractFilename(key);

                    Video video = new Video();
                    // 用 key 的 hash 作为临时 id（保证唯一，且不依赖数据库）
                    video.setId(String.valueOf(key.hashCode()));
                    video.setName(filename);
                    video.setUrl(ossUtil.buildSignedUrl(key));
                    video.setKey(key);
                    return video;
                })
                .collect(Collectors.toList());
    }
}