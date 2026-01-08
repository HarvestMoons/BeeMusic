package com.example.musicplayer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "meme")
@Getter
@Setter
public class Meme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, unique = true)
    private String key; // Full path in OSS, e.g., "meme/abc1234.jpg"

    @Transient
    private String url;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Meme() {
    }

    public Meme(String key) {
        this.key = key;
    }
}
