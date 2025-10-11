package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.example.musicplayer.model.Video;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class VideoService {

    private final OSS ossClient;
    private final String bucketName = "bees-bucket";
    private final String videoPrefix = "videos/";

    public VideoService(
            @Value("${aliyun.oss.endpoint}") String endpoint,
            @Value("${aliyun.oss.access-key}") String accessKey,
            @Value("${aliyun.oss.secret-key}") String secretKey) {
        this.ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
    }

    // 获取所有视频文件
    public List<Video> getVideo() {
        ObjectListing objectListing = ossClient.listObjects(bucketName, videoPrefix);
        List<Video> videos = new ArrayList<>();

        for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {
            String key = summary.getKey();
            if (key.toLowerCase().endsWith(".mp4")) {
                String filename = key.substring(key.lastIndexOf("/") + 1);
                String signedUrl = ossClient.generatePresignedUrl(
                        bucketName, key,
                        new Date(System.currentTimeMillis() + 60*60 * 1000*24)
                ).toString();
                Video video = new Video();
                video.setId(Integer.toHexString(key.hashCode()));
                video.setName(filename);
                video.setUrl(signedUrl);
                video.setKey(key);
                videos.add(video);
            }
        }
        System.out.println(videos);
        return videos;
    }
}
