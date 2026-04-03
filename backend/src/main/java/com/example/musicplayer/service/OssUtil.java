package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OssUtil {

    private static final String CDN_ENABLED_KEY = "site:config:cdn_enabled";

    private final OSS ossClient;
    private final StringRedisTemplate redisTemplate;

    @Getter
    @Value("${aliyun.oss.bucket-name:bees-bucket}")
    private String bucketName;

    @Value("${aliyun.oss.public-direct-base-url:}")
    private String publicDirectBaseUrl;

    @Value("${aliyun.oss.public-cdn-base-url:}")
    private String publicCdnBaseUrl;

    @Value("${aliyun.oss.cdn-enabled-default:true}")
    private boolean cdnEnabledDefault;

    public OssUtil(OSS ossClient, StringRedisTemplate redisTemplate) {
        this.ossClient = ossClient;
        this.redisTemplate = redisTemplate;
    }

    public String buildPublicUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return objectKey;
        }

        boolean cdnEnabled = isCdnEnabled();
        String configuredBaseUrl = cdnEnabled ? publicCdnBaseUrl : publicDirectBaseUrl;
        String fallbackBaseUrl = String.format("https://%s.oss-cn-heyuan.aliyuncs.com", bucketName);
        String normalizedBaseUrl = (configuredBaseUrl == null || configuredBaseUrl.isBlank())
                ? fallbackBaseUrl
                : configuredBaseUrl.replaceAll("/+$", "");
        String normalizedObjectKey = objectKey.replaceFirst("^/+", "");
        return normalizedBaseUrl + "/" + normalizedObjectKey;
    }

    public boolean isCdnEnabled() {
        String value = redisTemplate.opsForValue().get(CDN_ENABLED_KEY);
        return value == null ? cdnEnabledDefault : "true".equalsIgnoreCase(value);
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
                            .withMaxKeys(1000));

            for (OSSObjectSummary summary : objectListing.getObjectSummaries()) {
                if (suffix == null || suffix.isEmpty()
                        || summary.getKey().toLowerCase().endsWith(suffix.toLowerCase())) {
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
