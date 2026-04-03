package com.example.musicplayer.controller;

import com.example.musicplayer.enums.UserRole;
import com.example.musicplayer.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SiteConfigController {

    private static final String COMMENT_ENABLED_KEY = "site:config:comments_enabled";
    private static final String CDN_ENABLED_KEY = "site:config:cdn_enabled";

    private final StringRedisTemplate redisTemplate;
    private final com.example.musicplayer.service.SongService songService;
    private final boolean cdnEnabledDefault;

    public SiteConfigController(
            StringRedisTemplate redisTemplate,
            com.example.musicplayer.service.SongService songService,
            @org.springframework.beans.factory.annotation.Value("${aliyun.oss.cdn-enabled-default:true}") boolean cdnEnabledDefault) {
        this.redisTemplate = redisTemplate;
        this.songService = songService;
        this.cdnEnabledDefault = cdnEnabledDefault;
    }

    // 获取评论区状态 (公开接口)
    @GetMapping("/public/config/comments-enabled")
    public ResponseEntity<Map<String, Boolean>> getCommentsEnabled() {
        String value = redisTemplate.opsForValue().get(COMMENT_ENABLED_KEY);
        // 默认关闭 (false)
        boolean enabled = "true".equals(value);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @GetMapping("/public/config/cdn-enabled")
    public ResponseEntity<Map<String, Boolean>> getCdnEnabled() {
        String value = redisTemplate.opsForValue().get(CDN_ENABLED_KEY);
        boolean enabled = value == null ? cdnEnabledDefault : "true".equalsIgnoreCase(value);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    // 切换评论区状态 (仅站长)
    @PostMapping("/admin/config/comments-enabled")
    public ResponseEntity<?> setCommentsEnabled(@RequestBody Map<String, Boolean> body, HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute("user");
        if (!(userObj instanceof User user) || user.getRoleEnum() != UserRole.STATION_MASTER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only Station Master can change site config");
        }

        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body("Missing 'enabled' field");
        }

        redisTemplate.opsForValue().set(COMMENT_ENABLED_KEY, enabled.toString());
        return ResponseEntity.ok(Map.of("enabled", enabled, "message", "Configuration updated"));
    }

    @PostMapping("/admin/config/cdn-enabled")
    public ResponseEntity<?> setCdnEnabled(@RequestBody Map<String, Boolean> body, HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute("user");
        if (!(userObj instanceof User user) || user.getRoleEnum() != UserRole.STATION_MASTER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only Station Master can change site config");
        }

        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body("Missing 'enabled' field");
        }

        redisTemplate.opsForValue().set(CDN_ENABLED_KEY, enabled.toString());
        songService.evictAllFolderCaches();
        return ResponseEntity.ok(Map.of(
                "enabled", enabled,
                "message", "Media access mode updated and song caches cleared"));
    }
}
