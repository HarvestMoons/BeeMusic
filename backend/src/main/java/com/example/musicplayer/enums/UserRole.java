package com.example.musicplayer.enums;

public enum UserRole {
    USER(1),
    ADMIN(2),
    STATION_MASTER(3);

    private final int code;

    UserRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UserRole fromCode(int code) {
        for (UserRole role : values()) {
            if (role.getCode() == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role code: " + code);
    }
}
