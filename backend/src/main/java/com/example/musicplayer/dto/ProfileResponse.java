package com.example.musicplayer.dto;

import com.example.musicplayer.model.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户在"我的"页面展示的资料，只包含可对外暴露的字段。
 */
@Data
public class ProfileResponse {
    private String username;
    private Integer role;
    private LocalDateTime createdAt;

    public static ProfileResponse from(User user) {
        ProfileResponse response = new ProfileResponse();
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
