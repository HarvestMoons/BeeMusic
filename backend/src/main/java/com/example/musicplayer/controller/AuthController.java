package com.example.musicplayer.controller;

import com.example.musicplayer.dto.AuthResponse;
import com.example.musicplayer.dto.LoginRequest;
import com.example.musicplayer.dto.RegisterRequest;
import com.example.musicplayer.model.User;
import com.example.musicplayer.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse(true, "Registration successful", user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession session) {

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
        Authentication authentication = authenticationManager.authenticate(authToken);

        // 使用 SecurityContextHolderStrategy 创建并设置上下文
        SecurityContextHolderStrategy strategy = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = strategy.createEmptyContext();
        context.setAuthentication(authentication);
        strategy.setContext(context);

        // 显式保存 SecurityContext 到 session
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = userService.findByUsername(request.getUsername());
        userService.updateLastActiveTime(user);
        session.setAttribute("user", user);

        return ResponseEntity.ok(new AuthResponse(true, "Login successful", user.getUsername()));

    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        return ResponseEntity.ok(new AuthResponse(true, "Logout successful", null));
    }

    @GetMapping("/status")
    public ResponseEntity<AuthResponse> status(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            userService.updateLastActiveTime(user);
            return ResponseEntity.ok(new AuthResponse(true, "Authenticated", user.getUsername()));
        }
        return ResponseEntity.ok(new AuthResponse(false, "Not authenticated", null));
    }
}