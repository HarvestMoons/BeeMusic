package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class OssUtil {

    private final OSS ossClient;

    @Getter
    @Value("${aliyun.oss.bucket-name:bees-bucket}")
    private String bucketName;

    @Value("${aliyun.oss.expiration-hours:24}")
    private long expirationHours;

    public OssUtil(OSS ossClient) {
        this.ossClient = ossClient;
    }

    public Date getExpirationDate() {
        return new Date(System.currentTimeMillis() + expirationHours * 3600 * 1000);
    }

    public List<OSSObjectSummary> listFilesByPrefixAndSuffix(String prefix, String suffix) {
        List<OSSObjectSummary> results = new ArrayList<>();
        String nextMarker = null;
        ObjectListing objectListing;

        do {
            objectListing = ossClient.listObjects(
                    new ListObjectsRequest(bucketName)
                            .withPrefix(prefix)
                            .withMarker(nextMarker)
                            .withMaxKeys(1000)
            );

            for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {
                if (suffix == null || suffix.isEmpty() || summary.getKey().toLowerCase().endsWith(suffix.toLowerCase())) {
                    results.add(summary);
                }
            }
            nextMarker = objectListing.getNextMarker();
        } while (objectListing.isTruncated());

        return results;
    }

    public List<OSSObjectSummary> listFilesByPrefix(String prefix) {
        return listFilesByPrefixAndSuffix(prefix, null);
    }
}
