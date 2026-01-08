package com.example.musicplayer.controller;

import com.example.musicplayer.model.Meme;
import com.example.musicplayer.service.MemeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class MemeController {

    private final MemeService memeService;

    public MemeController(MemeService memeService) {
        this.memeService = memeService;
    }

    @GetMapping("/api/public/memes/random")
    public ResponseEntity<?> getRandomMeme() {
        Meme meme = memeService.getRandomMeme();
        if (meme == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(meme);
    }

    // 仅限管理员权限
    @PostMapping("/api/memes/sync")
    public ResponseEntity<?> syncMemes() {
        int count = memeService.syncMemes();
        return ResponseEntity.ok(Map.of("message", "Sync successful", "added", count));
    }

    @DeleteMapping("/api/memes/{id}")
    public ResponseEntity<?> deleteMeme(@PathVariable Long id) {
        memeService.deleteMeme(id);
        return ResponseEntity.ok(Map.of("message", "Meme deleted"));
    }
}
