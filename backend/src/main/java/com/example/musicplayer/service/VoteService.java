package com.example.musicplayer.service;

import com.example.musicplayer.model.SongVote;
import com.example.musicplayer.model.VoteType;
import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.repository.SongVoteRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class VoteService {
    private final SongVoteRepository songVoteRepository;
    private final SongRepository songRepository;
    private final StringRedisTemplate redisTemplate;

    public VoteService(SongVoteRepository songVoteRepository,
                       SongRepository songRepository,
                       StringRedisTemplate redisTemplate) {
        this.songVoteRepository = songVoteRepository;
        this.songRepository = songRepository;
        this.redisTemplate = redisTemplate;
    }

    private String likesKey(Long songId) { return "likes:" + songId; }
    private String dislikesKey(Long songId) { return "dislikes:" + songId; }

    @Transactional
    public Map<String, Integer> like(Long songId, Long userId) {
        return vote(songId, userId, VoteType.LIKE);
    }

    @Transactional
    public Map<String, Integer> dislike(Long songId, Long userId) {
        return vote(songId, userId, VoteType.DISLIKE);
    }

    @Transactional
    public Map<String, Integer> vote(Long songId, Long userId, VoteType newType) {
        // Validate song exists
        songRepository.findById(songId).orElseThrow(() -> new IllegalArgumentException("Song not found: " + songId));

        SongVote existing = songVoteRepository.findByUserIdAndSongId(userId, songId).orElse(null);
        if (existing == null) {
            SongVote sv = new SongVote();
            sv.setUserId(userId);
            sv.setSongId(songId);
            sv.setVoteType(newType);
            songVoteRepository.save(sv);
            addToRedis(songId, userId, newType);
        } else {
            VoteType oldType = existing.getVoteType();
            if (oldType != newType) {
                existing.setVoteType(newType);
                songVoteRepository.save(existing);
                switchRedis(songId, userId, oldType, newType);
            } // else idempotent
        }
        return counts(songId);
    }

    @Transactional
    public Map<String, Integer> cancel(Long songId, Long userId) {
        SongVote existing = songVoteRepository.findByUserIdAndSongId(userId, songId).orElse(null);
        if (existing != null) {
            VoteType oldType = existing.getVoteType();
            songVoteRepository.delete(existing);
            removeFromRedis(songId, userId, oldType);
        }
        return counts(songId);
    }

    public Map<String, Integer> counts(Long songId) {
        Long likeSize = redisTemplate.opsForSet().size(likesKey(songId));
        Long dislikeSize = redisTemplate.opsForSet().size(dislikesKey(songId));
        int likes = likeSize != null ? likeSize.intValue() : 0;
        int dislikes = dislikeSize != null ? dislikeSize.intValue() : 0;
        return Map.of("likes", likes, "dislikes", dislikes);
    }

    private void addToRedis(Long songId, Long userId, VoteType type) {
        if (type == VoteType.LIKE) {
            redisTemplate.opsForSet().add(likesKey(songId), userId.toString());
        } else {
            redisTemplate.opsForSet().add(dislikesKey(songId), userId.toString());
        }
    }

    private void switchRedis(Long songId, Long userId, VoteType oldType, VoteType newType) {
        if (oldType == VoteType.LIKE) {
            redisTemplate.opsForSet().remove(likesKey(songId), userId.toString());
        } else {
            redisTemplate.opsForSet().remove(dislikesKey(songId), userId.toString());
        }
        addToRedis(songId, userId, newType);
    }

    private void removeFromRedis(Long songId, Long userId, VoteType oldType) {
        if (oldType == VoteType.LIKE) {
            redisTemplate.opsForSet().remove(likesKey(songId), userId.toString());
        } else {
            redisTemplate.opsForSet().remove(dislikesKey(songId), userId.toString());
        }
    }
}

