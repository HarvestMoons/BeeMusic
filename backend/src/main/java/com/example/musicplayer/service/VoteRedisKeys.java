package com.example.musicplayer.service;

import com.example.musicplayer.enums.VoteType;

import java.util.List;

/**
 * 投票读模型在 Redis 中的键定义。
 * 集中在一处，避免 "likes:" / "dislikes:" 前缀散落在多个类里重复拼接导致不一致。
 */
public final class VoteRedisKeys {

    private static final String LIKES_PREFIX = "likes:";
    private static final String DISLIKES_PREFIX = "dislikes:";

    /** 扫描全部点赞键时使用的模式。 */
    public static final String LIKES_PATTERN = LIKES_PREFIX + "*";

    /** 扫描全部点踩键时使用的模式。 */
    public static final String DISLIKES_PATTERN = DISLIKES_PREFIX + "*";

    private VoteRedisKeys() {
    }

    public static String likes(Long songId) {
        return LIKES_PREFIX + songId;
    }

    public static String dislikes(Long songId) {
        return DISLIKES_PREFIX + songId;
    }

    public static String counts(Long songId) {
        return "votes:counts:" + songId;
    }

    /** 某首歌的点赞键 + 点踩键，用于一次性删除。 */
    public static List<String> all(Long songId) {
        return List.of(likes(songId), dislikes(songId), counts(songId));
    }

    /** 按投票类型取对应的键。 */
    public static String byType(Long songId, VoteType type) {
        return switch (type) {
            case LIKE -> likes(songId);
            case DISLIKE -> dislikes(songId);
        };
    }
}
