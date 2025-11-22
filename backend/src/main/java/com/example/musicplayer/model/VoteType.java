package com.example.musicplayer.model;

public enum VoteType {
    LIKE(1),
    DISLIKE(-1);

    private final int code;

    VoteType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static VoteType fromCode(int code) {
        if (code == 1) {
            return LIKE;
        }
        if (code == -1) {
            return DISLIKE;
        }
        throw new IllegalArgumentException("Invalid vote code: " + code);
    }
}

