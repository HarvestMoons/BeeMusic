package com.example.musicplayer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_song_listening_stats", uniqueConstraints = @UniqueConstraint(
        name = "uk_listening_user_song", columnNames = {"user_id", "song_id"}))
@Getter
@Setter
public class UserSongListeningStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false, updatable = false)
    private Long songId;

    @Column(name = "song_name", nullable = false, length = 255)
    private String songName;

    @Column(name = "listened_ms", nullable = false)
    private Long listenedMs = 0L;

    @Column(name = "first_listened_at", nullable = false, updatable = false)
    private LocalDateTime firstListenedAt;

    @Column(name = "last_listened_at", nullable = false)
    private LocalDateTime lastListenedAt;
}
