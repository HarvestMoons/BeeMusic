package com.example.musicplayer.controller;

import com.example.musicplayer.model.Video;
import com.example.musicplayer.service.VideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/random")
    public List<Video> getVideo() {
        return videoService.getVideos();
    }
}
