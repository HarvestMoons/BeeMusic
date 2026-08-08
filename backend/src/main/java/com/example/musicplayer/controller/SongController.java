package com.example.musicplayer.controller;

import com.example.musicplayer.dto.FolderSongCount;
import com.example.musicplayer.enums.UserRole;
import com.example.musicplayer.model.Song;
import com.example.musicplayer.service.SongService;
import com.example.musicplayer.service.CustomUserDetails;
import com.example.musicplayer.service.VoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SongController {

    private final SongService songService;
    private final VoteService voteService;

    public SongController(SongService songService, VoteService voteService) {
        this.songService = songService;
        this.voteService = voteService;
    }

    // 首页歌曲列表
    @GetMapping("/public/songs/get")
    public List<Song> getSongs(@RequestParam String folder,
                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean includeDeleted = false;
        if (userDetails != null && userDetails.isEnabled()) {
            UserRole role = userDetails.getUser().getRoleEnum();
            if (role == UserRole.ADMIN || role == UserRole.STATION_MASTER) {
                includeDeleted = true;
            }
        }
        return songService.getSongs(folder, includeDeleted);
    }

    // 删除歌曲（软删除）
    @PostMapping("/songs/delete/{songId}")
    public ResponseEntity<?> deleteSong(@PathVariable Long songId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (isStationMaster(userDetails)) {
            songService.deleteSong(songId);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "已删除"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权操作");
    }

    // 恢复歌曲
    @PostMapping("/songs/restore/{songId}")
    public ResponseEntity<?> restoreSong(@PathVariable Long songId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (isStationMaster(userDetails)) {
            songService.restoreSong(songId);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "已恢复"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权操作");
    }

    @GetMapping("/public/songs/folder-counts")
    public List<FolderSongCount> getFolderCounts() {
        return songService.getFolderSongCounts();
    }

    // 增加播放次数
    @PostMapping("/public/songs/play/{songId}")
    public void incrementPlayCount(@PathVariable Long songId) {
        songService.incrementPlayCount(songId);
    }

    // 获取点赞/点踩数
    @GetMapping("/public/songs/votes/{songId}")
    public Map<String, Integer> getVotes(@PathVariable Long songId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.isEnabled()) {
            return voteService.countsForUser(songId, userDetails.getUser().getId());
        }
        return voteService.counts(songId);
    }

    // 点赞
    @PostMapping("/songs/like/{songId}")
    public Map<String, Integer> likeSong(@PathVariable Long songId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        return voteService.like(songId, userId);
    }

    // 点踩
    @PostMapping("/songs/dislike/{songId}")
    public Map<String, Integer> dislikeSong(@PathVariable Long songId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        return voteService.dislike(songId, userId);
    }

    // 取消点赞/点踩
    @DeleteMapping("/songs/vote/{songId}")
    public Map<String, Integer> cancelVote(@PathVariable Long songId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        return voteService.cancel(songId, userId);
    }

    private Long currentUserId(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.isEnabled()) {
            return userDetails.getUser().getId();
        }
        throw new IllegalStateException("未登录");
    }

    // 手动触发 OSS 同步 (站长专用)
    @PostMapping("/admin/songs/sync")
    public ResponseEntity<?> syncSongs(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (isStationMaster(userDetails)) {
            songService.syncAllSongs();
            return ResponseEntity.ok(Map.of("status", "ok", "message", "数据库同步已触发"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only Station Master can sync database");
    }

    private boolean isStationMaster(CustomUserDetails userDetails) {
        return userDetails != null
                && userDetails.isEnabled()
                && userDetails.getUser().getRoleEnum() == UserRole.STATION_MASTER;
    }
}
