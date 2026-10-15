package com.example.musicplayer.repository;

import com.example.musicplayer.model.UserSongListeningStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSongListeningStatsRepository extends JpaRepository<UserSongListeningStats, Long> {
    Optional<UserSongListeningStats> findByUserIdAndSongId(Long userId, Long songId);

    @Query("""
            SELECT COALESCE(SUM(s.listenedMs), 0)
            FROM UserSongListeningStats s WHERE s.userId = :userId
            """)
    long totalListenedMs(@Param("userId") Long userId);

    @Query("""
            SELECT s FROM UserSongListeningStats s WHERE s.userId = :userId AND s.listenedMs > 0
            ORDER BY s.listenedMs DESC, s.lastListenedAt DESC, s.id DESC
            """)
    List<UserSongListeningStats> findFavorites(@Param("userId") Long userId, Pageable pageable);
}
