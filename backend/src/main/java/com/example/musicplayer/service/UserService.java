package com.example.musicplayer.service;

import com.example.musicplayer.enums.UserRole;
import com.example.musicplayer.exception.UserOperationException;
import com.example.musicplayer.model.User;
import com.example.musicplayer.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

@Service
public class UserService {

    /** 与 users.username 列的 length 保持一致。 */
    private static final int MAX_USERNAME_LENGTH = 64;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String username, String password) {
        username = username == null ? "" : username.trim();
        if (username.isBlank() || username.length() > MAX_USERNAME_LENGTH || username.chars().anyMatch(Character::isISOControl)) {
            throw new UserOperationException("用户名需为 1 到 64 个字符，且不能包含控制字符");
        }
        validatePassword(password);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UserOperationException("用户名已被占用");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setLastActiveTime(LocalDateTime.now());
        user.setRoleEnum(UserRole.USER);
        user.setIsHiddenPlaylistUnlocked(false);
        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * 修改密码。必须在事务外由调用方负责让其他会话失效。
     */
    @Transactional
    public User changePassword(Long userId, String oldPassword, String newPassword) {
        User user = activeUserForUpdate(userId);
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new UserOperationException("当前密码不正确");
        }
        validatePassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new UserOperationException("新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    /**
     * 修改用户名。调用方必须同步刷新当前会话的 SecurityContext，
     * 否则会话里缓存的旧用户名会让后续按用户名查库的接口失败。
     */
    @Transactional
    public User changeUsername(Long userId, String newUsername) {
        User user = activeUserForUpdate(userId);
        String trimmed = newUsername == null ? "" : newUsername.trim();
        if (trimmed.isEmpty()) {
            throw new UserOperationException("用户名不能为空");
        }
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            throw new UserOperationException("用户名不能超过 " + MAX_USERNAME_LENGTH + " 个字符");
        }
        if (trimmed.equals(user.getUsername())) {
            throw new UserOperationException("新用户名与当前用户名相同");
        }
        if (userRepository.findByUsername(trimmed).isPresent()) {
            throw new UserOperationException("用户名已被占用");
        }
        user.setUsername(trimmed);
        return userRepository.save(user);
    }

    @Transactional
    public void updateLastActiveTime(User user) {
        activeUserForUpdate(user.getId()).setLastActiveTime(LocalDateTime.now());
    }

    @Transactional
    public void unlockHiddenPlaylist(Long userId) {
        User user = activeUserForUpdate(userId);
        user.setIsHiddenPlaylistUnlocked(true);
        userRepository.save(user);
    }

    @Transactional
    public User deactivateAccount(Long userId, String username, String password) {
        User user = activeUserForUpdate(userId);
        if (user.getRoleEnum() == UserRole.STATION_MASTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "站长账户暂不支持注销");
        }
        if (!user.getUsername().equals(username)) {
            throw new UserOperationException("账户信息已变更，请刷新后重试");
        }
        if (password == null || password.isBlank() ||
                password.getBytes(StandardCharsets.UTF_8).length > 72 ||
                !passwordEncoder.matches(password, user.getPassword())) {
            throw new UserOperationException("当前密码不正确");
        }
        user.setStatus(0);
        return user;
    }

    private User activeUserForUpdate(Long userId) {
        return userRepository.findActiveLockedById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账户不可用，请重新登录"));
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) throw new UserOperationException("密码不能为空");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new UserOperationException("密码不能超过 72 个 UTF-8 字节");
        }
    }
}
