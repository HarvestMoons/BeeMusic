package com.example.musicplayer.repository;

import com.example.musicplayer.model.SongVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SongVoteRepository extends JpaRepository<SongVote, Long> {
    Optional<SongVote> findByUserIdAndSongId(Long userId, Long songId);

    /**
     * 删除某首歌曲的全部投票记录。
     * 数据库虽有 ON DELETE CASCADE，但这里显式删除，避免外键被改动后静默残留。
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SongVote sv WHERE sv.songId = :songId")
    int deleteBySongId(@Param("songId") Long songId);
}
