package com.example.musicplayer;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MusicPlayerApplication {

    @PostConstruct
    public void init() {
        // 设置默认时区为东八区 (Asia/Shanghai)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        SpringApplication.run(MusicPlayerApplication.class, args);
    }
}
