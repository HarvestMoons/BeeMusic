package com.example.musicplayer.controller;

import com.example.musicplayer.model.Song;
import com.example.musicplayer.model.SongVote;
import com.example.musicplayer.repository.SongVoteRepository;
import com.example.musicplayer.service.SongService;
import com.example.musicplayer.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SongController {

    private final SongService songService;
    private final VoteService voteService;
    private final SongVoteRepository songVoteRepository;

    public SongController(SongService songService, VoteService voteService, SongVoteRepository songVoteRepository) {
        this.songService = songService;
        this.voteService = voteService;
        this.songVoteRepository = songVoteRepository;
    }

    // 首页歌曲列表
    @GetMapping("/public/songs/get")
    public List<Song> getSongs() {
        return songService.getSongs();
    }

    // 切换歌曲文件夹
    @PostMapping("/public/songs/set-folder")
    public Map<String, String> setFolder(@RequestBody Map<String, String> body) {
        String folder = body.get("folder");
        return songService.setFolder(folder);
    }

    // 增加播放次数
    @PostMapping("/public/songs/play/{songId}")
    public void incrementPlayCount(@PathVariable Long songId) {
        songService.incrementPlayCount(songId);
    }

    // 获取点赞/点踩数
    @GetMapping("/public/songs/votes/{songId}")
    public Map<String, Integer> getVotes(@PathVariable Long songId, HttpServletRequest request) {
        String cookieHeader = request.getHeader("Cookie");
        System.out.println(cookieHeader);
        System.out.println(request.getSession(false));
        Map<String, Integer> counts = voteService.counts(songId);
        Object userObj = request.getSession().getAttribute("user");
        if (userObj instanceof com.example.musicplayer.model.User u) {
            var opt = songVoteRepository.findByUserIdAndSongId(u.getId(), songId);
            int userVote = opt.map(SongVote::getVote).orElse(0);
            counts = Map.of(
                    "likes", counts.get("likes"),
                    "dislikes", counts.get("dislikes"),
                    "userVote", userVote
            );
        }
        return counts;
    }

    // 点赞
    @PostMapping("/songs/like/{songId}")
    public Map<String, Integer> likeSong(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        return voteService.like(songId, userId);
    }

    // 点踩
    @PostMapping("/songs/dislike/{songId}")
    public Map<String, Integer> dislikeSong(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        return voteService.dislike(songId, userId);
    }

    // 取消点赞/点踩
    @DeleteMapping("/songs/vote/{songId}")
    public Map<String, Integer> cancelVote(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        return voteService.cancel(songId, userId);
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute("user");
        if (userObj instanceof com.example.musicplayer.model.User u) {
            return u.getId();
        }
        throw new IllegalStateException("未登录");
    }
}
