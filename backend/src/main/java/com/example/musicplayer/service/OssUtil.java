package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OssUtil {

    private final OSS ossClient;
    private final String bucketName = "bees-bucket";

    public OssUtil(OSS ossClient) {
        this.ossClient = ossClient;
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
                if (summary.getKey().toLowerCase().endsWith(suffix.toLowerCase())) {
                    results.add(summary);
                }
            }
            nextMarker = objectListing.getNextMarker();
        } while (objectListing.isTruncated());

        return results;
    }
}
