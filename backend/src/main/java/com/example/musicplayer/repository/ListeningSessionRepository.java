package com.example.musicplayer.repository;

import com.example.musicplayer.model.UserListeningSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ListeningSessionRepository extends JpaRepository<UserListeningSession, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM UserListeningSession p WHERE p.id = :id")
    Optional<UserListeningSession> findLockedById(@Param("id") String id);

    // 调用方已持有用户行锁，开始新会话时关闭同一用户的其他播放记录。
    @Modifying(flushAutomatically = true)
    @Query("UPDATE UserListeningSession p SET p.closed = true WHERE p.userId = :userId AND p.closed = false")
    int closeOpenSessions(@Param("userId") Long userId);
}
