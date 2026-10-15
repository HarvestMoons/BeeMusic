package com.example.musicplayer.service;

import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.repository.SongVoteRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class VoteRedisProjector {
    private static final String SAVE_COUNTS =
            "redis.call('set', KEYS[3], redis.call('scard', KEYS[1]) .. ':' .. redis.call('scard', KEYS[2])); return 1";
    static final DefaultRedisScript<Long> PROJECT_SCRIPT = new DefaultRedisScript<>(
            "redis.call('srem', KEYS[1], ARGV[2]); redis.call('srem', KEYS[2], ARGV[2]); " +
            "if ARGV[1] == '1' then redis.call('sadd', KEYS[1], ARGV[2]); " +
            "elseif ARGV[1] == '-1' then redis.call('sadd', KEYS[2], ARGV[2]); end; " + SAVE_COUNTS,
            Long.class);
    static final DefaultRedisScript<Long> REBUILD_SCRIPT = new DefaultRedisScript<>(
            "redis.call('del', KEYS[1], KEYS[2]); " +
            "for i = 1, #ARGV, 2 do " +
            "if ARGV[i] == '1' then redis.call('sadd', KEYS[1], ARGV[i+1]); " +
            "elseif ARGV[i] == '-1' then redis.call('sadd', KEYS[2], ARGV[i+1]); end; end; " + SAVE_COUNTS,
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SongRepository songRepository;
    private final SongVoteRepository voteRepository;
    private final TransactionTemplate transactionTemplate;

    public VoteRedisProjector(StringRedisTemplate redisTemplate, SongRepository songRepository,
                              SongVoteRepository voteRepository, PlatformTransactionManager transactionManager) {
        this.redisTemplate = redisTemplate;
        this.songRepository = songRepository;
        this.voteRepository = voteRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 与投票写入使用同一歌曲行锁；读取最新已提交状态，避免迟到的投影覆盖新投票。 */
    public void projectCurrent(Long songId, Long userId) {
        transactionTemplate.executeWithoutResult(status -> {
            if (songRepository.findLockedById(songId).isEmpty()) return;
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(VoteRedisKeys.counts(songId)))) {
                rebuildLocked(songId);
                return;
            }
            String vote = voteRepository.findVoteCode(userId, songId).map(Object::toString).orElse("0");
            redisTemplate.execute(PROJECT_SCRIPT, VoteRedisKeys.all(songId), vote, userId.toString());
        });
    }

    public void rebuild(Long songId) {
        transactionTemplate.executeWithoutResult(status -> {
            if (songRepository.findLockedById(songId).isPresent()) rebuildLocked(songId);
        });
    }

    private void rebuildLocked(Long songId) {
        List<String> arguments = new ArrayList<>();
        for (var vote : voteRepository.findMembersBySongId(songId)) {
            arguments.add(vote.getVote().toString());
            arguments.add(vote.getUserId().toString());
        }
        // 单首歌在 Lua 中同时替换两个 Set 和计数，读请求始终看到完整的旧值或新值。
        redisTemplate.execute(REBUILD_SCRIPT, VoteRedisKeys.all(songId), arguments.toArray());
    }
}
