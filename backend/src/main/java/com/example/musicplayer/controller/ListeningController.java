package com.example.musicplayer.controller;

import com.example.musicplayer.dto.ListeningReport;
import com.example.musicplayer.dto.ListeningStatsResponse;
import com.example.musicplayer.service.CustomUserDetails;
import com.example.musicplayer.service.ListeningService;
import com.example.musicplayer.service.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/listening")
@RequiredArgsConstructor
public class ListeningController {
    private final ListeningService service;
    public record StartRequest(String playbackId, Long songId) {}

    @PostMapping("/sessions")
    public Map<String, Boolean> start(@AuthenticationPrincipal CustomUserDetails user, @RequestBody StartRequest request) {
        boolean active = service.start(SecurityUtils.currentUserId(user), request.playbackId(), request.songId());
        return Map.of("success", true, "active", active);
    }

    @PostMapping("/sessions/{id}")
    public Map<String, Boolean> report(@AuthenticationPrincipal CustomUserDetails user, @PathVariable String id,
                                      @RequestBody ListeningReport report) {
        boolean active = service.report(SecurityUtils.currentUserId(user), id, report);
        return Map.of("success", true, "active", active);
    }

    @GetMapping("/summary")
    public ListeningStatsResponse summary(@AuthenticationPrincipal CustomUserDetails user) {
        return service.summary(SecurityUtils.currentUserId(user));
    }
}
