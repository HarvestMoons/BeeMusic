package com.example.musicplayer.service;

import com.example.musicplayer.enums.UserRole;

/**
 * 从 Spring Security 的 {@link CustomUserDetails} 读取当前用户信息的公共工具。
 * 集中一处，避免各 Controller 里重复实现登录校验与角色判断。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 取当前登录用户 id。
     *
     * @throws IllegalStateException 未登录或账号被禁用
     */
    public static Long currentUserId(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.isEnabled()) {
            return userDetails.getUser().getId();
        }
        throw new IllegalStateException("未登录");
    }

    /** 当前用户是否为站长。 */
    public static boolean isStationMaster(CustomUserDetails userDetails) {
        return userDetails != null
                && userDetails.isEnabled()
                && userDetails.getUser().getRoleEnum() == UserRole.STATION_MASTER;
    }

    /** 当前用户是否为管理员或站长（例如用于查看已软删除的歌曲）。 */
    public static boolean isAdminOrStationMaster(CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.isEnabled()) {
            return false;
        }
        UserRole role = userDetails.getUser().getRoleEnum();
        return role == UserRole.ADMIN || role == UserRole.STATION_MASTER;
    }
}
