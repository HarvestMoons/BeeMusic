package com.example.musicplayer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private Integer role;

    @JsonProperty("isHiddenPlaylistUnlocked")
    private boolean isHiddenPlaylistUnlocked;

    public AuthResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        this.username = username;
    }

    public AuthResponse(boolean success, String message, String username, Integer role, boolean isHiddenPlaylistUnlocked) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.role = role;
        this.isHiddenPlaylistUnlocked = isHiddenPlaylistUnlocked;
    }
}