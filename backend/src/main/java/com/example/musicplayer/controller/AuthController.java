package com.example.musicplayer.controller;

import com.example.musicplayer.dto.AuthResponse;
import com.example.musicplayer.dto.LoginRequest;
import com.example.musicplayer.dto.RegisterRequest;
import com.example.musicplayer.model.User;
import com.example.musicplayer.service.CustomUserDetails;
import com.example.musicplayer.service.UserService;
import com.example.musicplayer.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.CacheControl;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserSessionService userSessionService;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final LogoutHandler logoutHandler;

    public AuthController(AuthenticationManager authenticationManager, UserService userService,
                          UserSessionService userSessionService,
                          SecurityContextRepository securityContextRepository,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy,
                          LogoutHandler apiLogoutHandler) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userSessionService = userSessionService;
        this.securityContextRepository = securityContextRepository;
        this.securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.logoutHandler = apiLogoutHandler;
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(csrfToken);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(true, "Registration successful", user.getUsername(), user.getRole(), user.getIsHiddenPlaylistUnlocked()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authToken);
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = userService.findById(principal.getUser().getId());
        userService.updateLastActiveTime(user);

        try {
            // 自定义登录入口必须主动调用会话策略，轮换已有 Session ID 并清除旧 CSRF Token。
            sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            // 同一账号继续只保留最近一次登录的会话。
            userSessionService.invalidateOtherSessions(user.getUsername(), httpRequest);
        } catch (RuntimeException error) {
            // 登录初始化未完成时撤销当前认证，避免接口报错却留下已登录 Session。
            logoutHandler.logout(httpRequest, httpResponse, authentication);
            throw error;
        }

        return ResponseEntity.ok(new AuthResponse(true, "Login successful", user.getUsername(), user.getRole(), user.getIsHiddenPlaylistUnlocked()));

    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletRequest request, HttpServletResponse response,
                                               Authentication authentication) {
        logoutHandler.logout(request, response, authentication);
        return ResponseEntity.ok(new AuthResponse(true, "Logout successful", null));
    }

    @GetMapping("/status")
    public ResponseEntity<AuthResponse> status(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.isEnabled()) {
            User user = userService.findByUsername(userDetails.getUsername());
            userService.updateLastActiveTime(user);
            return ResponseEntity.ok(new AuthResponse(true, "Authenticated", user.getUsername(),
                    user.getRole(), user.getIsHiddenPlaylistUnlocked()));
        }
        return ResponseEntity.ok(new AuthResponse(false, "Not authenticated", null));
    }
}
