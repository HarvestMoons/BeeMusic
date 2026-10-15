package com.example.musicplayer.service;

import com.example.musicplayer.exception.UserOperationException;
import com.example.musicplayer.model.*;
import com.example.musicplayer.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"}, showSql = false)
@Import({VoteService.class, CommentService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InteractionIntegrationTest {
    @Autowired SongRepository songs;
    @Autowired SongVoteRepository votes;
    @Autowired CommentRepository comments;
    @Autowired CommentLikeRepository likes;
    @Autowired UserRepository users;
    @Autowired VoteService voteService;
    @Autowired CommentService commentService;
    @MockBean VoteRedisProjector projector;
    @MockBean StringRedisTemplate redis;
    Song song;
    User user;

    @BeforeEach
    void seed() {
        likes.deleteAll();
        comments.deleteAll();
        votes.deleteAll();
        songs.deleteAll();
        users.deleteAll();
        song = new Song();
        song.setName("test.mp3");
        song.setKey("music/test/test.mp3");
        song = songs.save(song);
        user = new User();
        user.setUsername("listener");
        user.setPassword("test-hash");
        user = users.save(user);
    }

    @Test
    void concurrentFirstVotesAreIdempotent() throws Exception {
        concurrently(12, () -> voteService.like(song.getId(), user.getId()));
        assertThat(votes.count()).isEqualTo(1);
        assertThat(voteService.countsForUser(song.getId(), user.getId()))
                .containsEntry("likes", 1).containsEntry("dislikes", 0).containsEntry("userVote", 1);
    }

    @Test
    void switchingAndCancellingVotesKeepsOneRelationship() throws Exception {
        voteService.like(song.getId(), user.getId());
        concurrently(12, () -> voteService.dislike(song.getId(), user.getId()));
        assertThat(votes.count()).isEqualTo(1);
        assertThat(voteService.countsForUser(song.getId(), user.getId()))
                .containsEntry("likes", 0).containsEntry("dislikes", 1).containsEntry("userVote", -1);
        concurrently(12, () -> voteService.cancel(song.getId(), user.getId()));
        assertThat(votes.count()).isZero();
    }

    @Test
    void committedVoteSucceedsWhenRedisProjectionFails() {
        doThrow(new IllegalStateException("Redis unavailable")).when(projector).projectCurrent(anyLong(), anyLong());
        assertThat(voteService.like(song.getId(), user.getId()))
                .containsEntry("likes", 1).containsEntry("userVote", 1);
        assertThat(votes.count()).isEqualTo(1);
    }

    @Test
    void commentLikesReturnFreshCountsAndRepeatedRequestsDoNotAddVotes() throws Exception {
        Long id = commentService.addComment(user.getId(), song.getId(), "hello", null, null).getId();
        assertThat(commentService.likeComment(user.getId(), id)).containsEntry("likeCount", 1);
        concurrently(12, () -> commentService.likeComment(user.getId(), id));
        assertThat(likes.count()).isEqualTo(1);
        assertThat(comments.findLikeCount(id)).isEqualTo(1);
        assertThat(commentService.unlikeComment(user.getId(), id)).containsEntry("likeCount", 0);
        assertThat(commentService.unlikeComment(user.getId(), id)).containsEntry("likeCount", 0);
    }

    @Test
    void rejectsInvalidCommentsAndCrossSongReplies() {
        assertThatThrownBy(() -> commentService.addComment(user.getId(), song.getId(), "  ", null, null))
                .isInstanceOf(UserOperationException.class);
        assertThatThrownBy(() -> commentService.addComment(user.getId(), song.getId(), "x".repeat(1001), null, null))
                .isInstanceOf(UserOperationException.class);
        Long parentId = commentService.addComment(user.getId(), song.getId(), "root", null, null).getId();
        Song other = new Song();
        other.setName("other.mp3");
        Long otherId = songs.save(other).getId();
        assertThatThrownBy(() -> commentService.addComment(user.getId(), otherId, "reply", parentId, user.getId()))
                .isInstanceOf(UserOperationException.class);
        assertThatThrownBy(() -> commentService.addComment(user.getId(), Long.MAX_VALUE, "hello", null, null))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(comments.count()).isEqualTo(1);
    }

    @Test
    void rejectsDeletingOthersCommentsAndReplyingToDeletedComments() {
        Long id = commentService.addComment(user.getId(), song.getId(), "hello", null, null).getId();
        assertThatThrownBy(() -> commentService.deleteComment(user.getId() + 1, id))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        commentService.deleteComment(user.getId(), id);
        assertThatThrownBy(() -> commentService.addComment(user.getId(), song.getId(), "reply", id, user.getId()))
                .isInstanceOf(ResponseStatusException.class);
    }

    private void concurrently(int count, Runnable operation) throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(count)) {
            CountDownLatch ready = new CountDownLatch(count);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                    operation.run();
                    return null;
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
        }
    }
}
