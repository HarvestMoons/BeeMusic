package com.example.musicplayer.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String role;

    public AuthResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        this.username = username;
    }

    public AuthResponse(boolean success, String message, String username, String role) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.role = role;
    }
}