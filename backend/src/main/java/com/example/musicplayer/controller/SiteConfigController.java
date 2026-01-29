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

    private final StringRedisTemplate redisTemplate;
    private static final String COMMENT_ENABLED_KEY = "site:config:comments_enabled";

    public SiteConfigController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 获取评论区状态 (公开接口)
    @GetMapping("/public/config/comments-enabled")
    public ResponseEntity<Map<String, Boolean>> getCommentsEnabled() {
        String value = redisTemplate.opsForValue().get(COMMENT_ENABLED_KEY);
        // 默认关闭 (false)
        boolean enabled = "true".equals(value);
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
}
