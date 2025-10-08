package com.example.musicplayer.controller;

import com.example.musicplayer.model.Song;
import com.example.musicplayer.service.SongService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }

    // 首页歌曲列表
    @GetMapping("/get")
    public List<Song> getSongs() {
        return songService.getSongs();
    }

    // 切换歌曲文件夹
    @PostMapping("/set-folder")
    public Map<String, Object> setFolder(@RequestBody Map<String, String> body) {
        String folder = body.get("folder");
        return songService.setFolder(folder);
    }

    // 获取点赞/点踩数
    @GetMapping("/votes/{songId}")
    public Map<String, Integer> getVotes(@PathVariable String songId) {
        return songService.getVotes(songId);
    }

    // 点赞
    @PostMapping("/like/{songId}")
    public Map<String, Integer> likeSong(@PathVariable String songId) {
        return songService.likeSong(songId);
    }

    // 点踩
    @PostMapping("/dislike/{songId}")
    public Map<String, Integer> dislikeSong(@PathVariable String songId) {
        return songService.dislikeSong(songId);
    }
}
