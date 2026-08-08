package com.example.musicplayer.service;

import com.example.musicplayer.enums.VoteType;
import com.example.musicplayer.model.SongVote;
import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.repository.SongVoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class VoteService {
    private final SongVoteRepository songVoteRepository;
    private final SongRepository songRepository;
    private final StringRedisTemplate redisTemplate;
    private final VoteRedisProjector redisProjector;
    private final TransactionTemplate transactionTemplate;

    public VoteService(SongVoteRepository songVoteRepository,
                       SongRepository songRepository,
                       StringRedisTemplate redisTemplate,
                       VoteRedisProjector redisProjector,
                       PlatformTransactionManager transactionManager) {
        this.songVoteRepository = songVoteRepository;
        this.songRepository = songRepository;
        this.redisTemplate = redisTemplate;
        this.redisProjector = redisProjector;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private String likesKey(Long songId) {
        return "likes:" + songId;
    }

    private String dislikesKey(Long songId) {
        return "dislikes:" + songId;
    }

    public Map<String, Integer> like(Long songId, Long userId) {
        return vote(songId, userId, VoteType.LIKE);
    }

    public Map<String, Integer> dislike(Long songId, Long userId) {
        return vote(songId, userId, VoteType.DISLIKE);
    }

    public Map<String, Integer> vote(Long songId, Long userId, VoteType newType) {
        VoteChange change = transactionTemplate.execute(status -> saveVote(songId, userId, newType));
        if (change != null) {
            projectAfterCommit(change);
        }
        return counts(songId);
    }

    public Map<String, Integer> cancel(Long songId, Long userId) {
        VoteChange change = transactionTemplate.execute(status -> cancelVote(songId, userId));
        if (change != null) {
            projectAfterCommit(change);
        }
        return counts(songId);
    }

    private VoteChange saveVote(Long songId, Long userId, VoteType newType) {
        songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));

        SongVote existing = songVoteRepository.findByUserIdAndSongId(userId, songId).orElse(null);
        if (existing == null) {
            existing = new SongVote();
            existing.setUserId(userId);
            existing.setSongId(songId);
        }
        if (existing.getVoteType() == newType) {
            return null;
        }
        existing.setVoteType(newType);
        songVoteRepository.save(existing);
        return new VoteChange(songId, userId, newType.getCode());
    }

    private VoteChange cancelVote(Long songId, Long userId) {
        SongVote existing = songVoteRepository.findByUserIdAndSongId(userId, songId).orElse(null);
        if (existing == null) {
            return null;
        }
        songVoteRepository.delete(existing);
        return new VoteChange(songId, userId, null);
    }

    private void projectAfterCommit(VoteChange change) {
        try {
            redisProjector.project(change.songId(), change.userId(), change.vote());
        } catch (RuntimeException error) {
            log.error("投票已写入 MySQL，但 Redis 更新失败；等待定期重建: songId={}, userId={}",
                    change.songId(), change.userId(), error);
        }
    }

    public Map<String, Integer> counts(Long songId) {
        Long likeSize = redisTemplate.opsForSet().size(likesKey(songId));
        Long dislikeSize = redisTemplate.opsForSet().size(dislikesKey(songId));
        int likes = likeSize != null ? likeSize.intValue() : 0;
        int dislikes = dislikeSize != null ? dislikeSize.intValue() : 0;
        return Map.of("likes", likes, "dislikes", dislikes);
    }

    public Map<String, Integer> countsForUser(Long songId, Long userId) {
        Map<String, Integer> counts = counts(songId);
        int userVote = songVoteRepository.findByUserIdAndSongId(userId, songId)
                .map(SongVote::getVote)
                .orElse(0);
        return Map.of(
                "likes", counts.get("likes"),
                "dislikes", counts.get("dislikes"),
                "userVote", userVote);
    }

    private record VoteChange(Long songId, Long userId, Integer vote) {
    }

}
