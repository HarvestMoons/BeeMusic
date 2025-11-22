package com.example.musicplayer.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key}")
    private String accessKey;

    @Value("${aliyun.oss.secret-key}")
    private String secretKey;

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKey, secretKey);
    }
}
