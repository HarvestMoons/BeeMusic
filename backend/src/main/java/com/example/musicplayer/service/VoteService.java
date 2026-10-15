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

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    public Map<String, Integer> like(Long songId, Long userId) {
        return vote(songId, userId, VoteType.LIKE);
    }

    public Map<String, Integer> dislike(Long songId, Long userId) {
        return vote(songId, userId, VoteType.DISLIKE);
    }

    public Map<String, Integer> vote(Long songId, Long userId, VoteType newType) {
        transactionTemplate.executeWithoutResult(status -> saveVote(songId, userId, newType));
        projectAfterCommit(songId, userId);
        return databaseState(songId, userId);
    }

    public Map<String, Integer> cancel(Long songId, Long userId) {
        transactionTemplate.executeWithoutResult(status -> cancelVote(songId, userId));
        projectAfterCommit(songId, userId);
        return databaseState(songId, userId);
    }

    private void lockActiveSong(Long songId) {
        var song = songRepository.findLockedById(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "歌曲不存在"));
        if (song.getIsDeleted() != 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "歌曲已删除");
        }
    }

    private void saveVote(Long songId, Long userId, VoteType newType) {
        lockActiveSong(songId);

        SongVote existing = songVoteRepository.findByUserIdAndSongId(userId, songId).orElse(null);
        if (existing == null) {
            existing = new SongVote();
            existing.setUserId(userId);
            existing.setSongId(songId);
        }
        if (Objects.equals(existing.getVote(), newType.getCode())) {
            return;
        }
        existing.setVoteType(newType);
        songVoteRepository.save(existing);
    }

    private void cancelVote(Long songId, Long userId) {
        lockActiveSong(songId);
        SongVote existing = songVoteRepository.findByUserIdAndSongId(userId, songId).orElse(null);
        if (existing == null) {
            return;
        }
        songVoteRepository.delete(existing);
    }

    private void projectAfterCommit(Long songId, Long userId) {
        try {
            redisProjector.projectCurrent(songId, userId);
        } catch (RuntimeException error) {
            log.error("投票已写入 MySQL，但 Redis 更新失败；等待定期重建: songId={}, userId={}",
                    songId, userId, error);
            try {
                redisTemplate.delete(VoteRedisKeys.counts(songId));
            } catch (RuntimeException invalidationError) {
                log.warn("投票缓存失效失败: songId={}", songId, invalidationError);
            }
        }
    }

    public Map<String, Integer> counts(Long songId) {
        return countsForSongs(List.of(songId)).get(songId);
    }

    public Map<String, Integer> countsForUser(Long songId, Long userId) {
        return databaseState(songId, userId);
    }

    private Map<String, Integer> databaseState(Long songId, Long userId) {
        return transactionTemplate.execute(status -> {
            Map<String, Integer> counts = databaseCounts(List.of(songId)).get(songId);
            int userVote = songVoteRepository.findVoteCode(userId, songId).orElse(0);
            return Map.of("likes", counts.get("likes"), "dislikes", counts.get("dislikes"), "userVote", userVote);
        });
    }

    /** MGET 读取原子计数字符串；Redis 重启、缺失或故障时按批次回源 MySQL。 */
    public Map<Long, Map<String, Integer>> countsForSongs(List<Long> songIds) {
        Map<Long, Map<String, Integer>> result = new HashMap<>();
        if (songIds.isEmpty()) return result;
        try {
            List<String> cached = redisTemplate.opsForValue().multiGet(songIds.stream().map(VoteRedisKeys::counts).toList());
            for (int i = 0; cached != null && i < songIds.size(); i++) {
                String value = cached.get(i);
                if (value == null) continue;
                String[] parts = value.split(":");
                if (parts.length == 2) {
                    result.put(songIds.get(i), Map.of("likes", Integer.parseInt(parts[0]), "dislikes", Integer.parseInt(parts[1])));
                }
            }
        } catch (RuntimeException error) {
            log.warn("读取投票缓存失败，回源 MySQL", error);
            result.clear();
        }
        List<Long> missing = songIds.stream().filter(id -> !result.containsKey(id)).toList();
        if (!missing.isEmpty()) result.putAll(databaseCounts(missing));
        return result;
    }

    private Map<Long, Map<String, Integer>> databaseCounts(List<Long> songIds) {
        Map<Long, Map<String, Integer>> result = new HashMap<>();
        for (Long id : songIds) result.put(id, new HashMap<>(Map.of("likes", 0, "dislikes", 0)));
        for (var count : songVoteRepository.countVotes(songIds)) {
            result.get(count.getSongId()).put(count.getVote() == 1 ? "likes" : "dislikes", Math.toIntExact(count.getTotal()));
        }
        return result;
    }

}
