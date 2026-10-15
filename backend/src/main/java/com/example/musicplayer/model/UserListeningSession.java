package com.example.musicplayer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_listening_sessions")
@Getter
@Setter
public class UserListeningSession implements Persistable<String> {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false, updatable = false)
    private Long songId;

    @Column(name = "song_name", nullable = false, length = 255)
    private String songName;

    @Column(name = "listened_ms", nullable = false)
    private Long listenedMs = 0L;

    @Column(name = "client_listened_ms", nullable = false)
    private Long clientListenedMs = 0L;

    @Column(name = "last_sequence", nullable = false)
    private Long lastSequence = 0L;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private boolean closed;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_reported_at", nullable = false)
    private LocalDateTime lastReportedAt;

    // UUID 由播放器分配。显式区分新实体，使首次保存使用 INSERT，避免 merge 覆盖同 ID 记录。
    @Transient
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    private void markPersisted() {
        newEntity = false;
    }
}
