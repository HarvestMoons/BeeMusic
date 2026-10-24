package com.example.musicplayer.config;

import com.example.musicplayer.repository.UserRepository;
import com.example.musicplayer.service.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Session 中的用户是快照，注销后必须按数据库状态拒绝尚未失效的旧会话。 */
@RequiredArgsConstructor
public class ActiveUserFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final LogoutHandler logoutHandler;
    private final AuthenticationEntryPoint entryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user &&
                !userRepository.existsByIdAndStatus(user.getUser().getId(), 1)) {
            logoutHandler.logout(request, response, authentication);
            entryPoint.commence(request, response, new DisabledException("账户已注销或禁用"));
            return;
        }
        chain.doFilter(request, response);
    }
}
