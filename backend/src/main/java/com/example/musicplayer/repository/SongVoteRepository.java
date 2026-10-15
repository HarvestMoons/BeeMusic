package com.example.musicplayer.repository;

import com.example.musicplayer.model.SongVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface SongVoteRepository extends JpaRepository<SongVote, Long> {
    Optional<SongVote> findByUserIdAndSongId(Long userId, Long songId);

    interface VoteMember {
        Long getUserId();
        Integer getVote();
    }

    @Query("SELECT v.userId AS userId, v.vote AS vote FROM SongVote v WHERE v.songId = :songId")
    List<VoteMember> findMembersBySongId(@Param("songId") Long songId);

    @Query("SELECT v.vote FROM SongVote v WHERE v.userId = :userId AND v.songId = :songId")
    Optional<Integer> findVoteCode(@Param("userId") Long userId, @Param("songId") Long songId);

    interface VoteCount {
        Long getSongId();
        Integer getVote();
        long getTotal();
    }

    @Query("SELECT v.songId AS songId, v.vote AS vote, COUNT(v) AS total FROM SongVote v " +
            "WHERE v.songId IN :songIds GROUP BY v.songId, v.vote")
    List<VoteCount> countVotes(@Param("songIds") Collection<Long> songIds);

    /**
     * 删除某首歌曲的全部投票记录。
     * 数据库虽有 ON DELETE CASCADE，但这里显式删除，避免外键被改动后静默残留。
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SongVote sv WHERE sv.songId = :songId")
    int deleteBySongId(@Param("songId") Long songId);
}
