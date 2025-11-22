package com.example.musicplayer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "song_votes", uniqueConstraints = @UniqueConstraint(name = "uk_song_votes_user_song", columnNames = {"user_id", "song_id"}))
@Getter
@Setter
public class SongVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "vote", nullable = false)
    private Integer vote; // 1 like, -1 dislike

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public VoteType getVoteType() {
        return VoteType.fromCode(vote);
    }

    public void setVoteType(VoteType type) {
        this.vote = type.getCode();
    }
}

