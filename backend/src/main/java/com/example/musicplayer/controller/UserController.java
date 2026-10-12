package com.example.musicplayer.controller;

import com.example.musicplayer.dto.AuthResponse;
import com.example.musicplayer.dto.ChangePasswordRequest;
import com.example.musicplayer.dto.ChangeUsernameRequest;
import com.example.musicplayer.dto.ProfileResponse;
import com.example.musicplayer.model.User;
import com.example.musicplayer.service.CustomUserDetails;
import com.example.musicplayer.service.UserService;
import com.example.musicplayer.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserSessionService userSessionService;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    public UserController(UserService userService,
                          UserSessionService userSessionService,
                          SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.userSessionService = userSessionService;
        this.securityContextRepository = securityContextRepository;
        this.securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    }

    // 当前登录用户的资料
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || !userDetails.isEnabled()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(ProfileResponse.from(userService.findById(userDetails.getUser().getId())));
    }

    // 修改密码：保留当前会话，其他会话失效
    @PostMapping("/me/password")
    public ResponseEntity<AuthResponse> changePassword(@RequestBody ChangePasswordRequest request,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                                       HttpServletRequest httpRequest) {
        if (userDetails == null || !userDetails.isEnabled()) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.changePassword(userDetails.getUser().getId(),
                request.getOldPassword(), request.getNewPassword());
        userSessionService.invalidateOtherSessions(user.getUsername(), httpRequest);
        return ResponseEntity.ok(successResponse("密码修改成功", user));
    }

    // 修改用户名：当前会话立刻换用新用户名，其他会话失效
    @PostMapping("/me/username")
    public ResponseEntity<AuthResponse> changeUsername(@RequestBody ChangeUsernameRequest request,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                                       HttpServletRequest httpRequest,
                                                       HttpServletResponse httpResponse) {
        if (userDetails == null || !userDetails.isEnabled()) {
            return ResponseEntity.status(401).build();
        }
        String previousUsername = userDetails.getUsername();
        User user = userService.changeUsername(userDetails.getUser().getId(), request.getNewUsername());
        // 其他会话里缓存的仍是旧用户名，之后按用户名查库会失败，一并清理掉
        userSessionService.invalidateOtherSessions(previousUsername, httpRequest);
        refreshAuthentication(user, httpRequest, httpResponse);
        return ResponseEntity.ok(successResponse("用户名修改成功", user));
    }

    @PostMapping("/unlock-hidden-playlist")
    public ResponseEntity<String> unlockHiddenPlaylist(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        userService.unlockHiddenPlaylist(user.getId());
        return ResponseEntity.ok("Hidden playlist unlocked");
    }

    private AuthResponse successResponse(String message, User user) {
        return new AuthResponse(true, message, user.getUsername(),
                user.getRole(), user.getIsHiddenPlaylistUnlocked());
    }

    /**
     * 用最新的用户信息替换当前会话里的 Authentication。
     * 会话中保存的 {@link CustomUserDetails} 是登录时的快照，改名后必须重建；
     * 重新保存 SecurityContext 也会让 Spring Session 把 principal 索引迁到新用户名。
     */
    private void refreshAuthentication(User user, HttpServletRequest request, HttpServletResponse response) {
        Authentication current = securityContextHolderStrategy.getContext().getAuthentication();
        CustomUserDetails refreshed = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken newAuth = UsernamePasswordAuthenticationToken.authenticated(
                refreshed, current == null ? null : current.getCredentials(), refreshed.getAuthorities());
        if (current != null) {
            newAuth.setDetails(current.getDetails());
        }
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(newAuth);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
