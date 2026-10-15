package com.example.musicplayer.service;

import com.example.musicplayer.model.Song;
import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.repository.SongVoteRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** ICE_TEST_REDIS_PORT 指向独立测试 Redis；仅操作测试歌曲的键。 */
@EnabledIfEnvironmentVariable(named = "ICE_TEST_REDIS_PORT", matches = "[0-9]+")
class VoteRedisIntegrationTest {
    private static final Long SONG_ID = 9223372036854775800L;
    private LettuceConnectionFactory connection;
    private StringRedisTemplate redis;
    private SongRepository songs;
    private SongVoteRepository votes;
    private VoteRedisProjector projector;

    @BeforeEach
    void setup() {
        connection = new LettuceConnectionFactory("127.0.0.1", Integer.parseInt(System.getenv("ICE_TEST_REDIS_PORT")));
        connection.afterPropertiesSet();
        redis = new StringRedisTemplate(connection);
        songs = mock(SongRepository.class);
        votes = mock(SongVoteRepository.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(songs.findLockedById(SONG_ID)).thenReturn(Optional.of(new Song()));
        projector = new VoteRedisProjector(redis, songs, votes, transactions);
        redis.delete(VoteRedisKeys.all(SONG_ID));
    }

    @AfterEach
    void cleanup() {
        redis.delete(VoteRedisKeys.all(SONG_ID));
        connection.destroy();
    }

    @Test
    void lateProjectionReadsCurrentDatabaseVoteAndKeepsSetsExclusive() {
        redis.opsForSet().add(VoteRedisKeys.likes(SONG_ID), "42");
        redis.opsForValue().set(VoteRedisKeys.counts(SONG_ID), "1:0");
        when(votes.findVoteCode(42L, SONG_ID)).thenReturn(Optional.of(-1));
        projector.projectCurrent(SONG_ID, 42L);
        assertThat(redis.opsForSet().members(VoteRedisKeys.likes(SONG_ID))).isEmpty();
        assertThat(redis.opsForSet().members(VoteRedisKeys.dislikes(SONG_ID))).containsExactly("42");
        assertThat(redis.opsForValue().get(VoteRedisKeys.counts(SONG_ID))).isEqualTo("0:1");
        var order = inOrder(songs, votes);
        order.verify(songs).findLockedById(SONG_ID);
        order.verify(votes).findVoteCode(42L, SONG_ID);
        when(votes.findVoteCode(42L, SONG_ID)).thenReturn(Optional.empty());
        projector.projectCurrent(SONG_ID, 42L);
        assertThat(redis.opsForValue().get(VoteRedisKeys.counts(SONG_ID))).isEqualTo("0:0");
    }

    @Test
    void missingCacheRebuildsAllVotersIncludingZeroVoteSongs() {
        when(votes.findMembersBySongId(SONG_ID)).thenReturn(List.of(member(1L, 1), member(2L, -1)));
        projector.projectCurrent(SONG_ID, 1L);
        assertThat(redis.opsForValue().get(VoteRedisKeys.counts(SONG_ID))).isEqualTo("1:1");
        when(votes.findMembersBySongId(SONG_ID)).thenReturn(List.of());
        projector.rebuild(SONG_ID);
        assertThat(redis.opsForValue().get(VoteRedisKeys.counts(SONG_ID))).isEqualTo("0:0");
        assertThat(redis.opsForSet().members(VoteRedisKeys.likes(SONG_ID))).isEmpty();
        assertThat(redis.opsForSet().members(VoteRedisKeys.dislikes(SONG_ID))).isEmpty();
    }

    @Test
    void concurrentReadersNeverSeeClearedCountsDuringRebuild() throws Exception {
        redis.opsForValue().set(VoteRedisKeys.counts(SONG_ID), "1:0");
        when(votes.findMembersBySongId(SONG_ID)).thenReturn(List.of(member(1L, 1)));
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> writer = executor.submit(() -> {
                for (int i = 0; i < 100; i++) projector.rebuild(SONG_ID);
            });
            for (int i = 0; i < 300; i++) {
                assertThat(redis.opsForValue().get(VoteRedisKeys.counts(SONG_ID))).isEqualTo("1:0");
            }
            writer.get(20, TimeUnit.SECONDS);
        }
    }

    private SongVoteRepository.VoteMember member(Long userId, int code) {
        return new SongVoteRepository.VoteMember() {
            public Long getUserId() { return userId; }
            public Integer getVote() { return code; }
        };
    }
}
