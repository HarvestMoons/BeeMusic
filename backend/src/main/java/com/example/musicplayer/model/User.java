package com.example.musicplayer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @JsonIgnore
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_active_time", nullable = false)
    private LocalDateTime lastActiveTime;

    @Column(nullable = false)
    private Integer status = 1; // 1=active,0=disabled

    @PrePersist
    public void prePersist() {
        if (lastActiveTime == null) {
            lastActiveTime = LocalDateTime.now();
        }
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        lastActiveTime = LocalDateTime.now();
    }
}
