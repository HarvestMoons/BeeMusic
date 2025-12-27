package com.example.musicplayer.repository;

import com.example.musicplayer.model.SongVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongVoteRepository extends JpaRepository<SongVote, Long> {
    Optional<SongVote> findByUserIdAndSongId(Long userId, Long songId);
}

