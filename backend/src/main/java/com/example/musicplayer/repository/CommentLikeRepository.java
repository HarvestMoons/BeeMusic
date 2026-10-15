package com.example.musicplayer.repository;

import com.example.musicplayer.model.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CommentLike cl WHERE cl.userId = :userId AND cl.commentId = :commentId")
    int deleteByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Query("SELECT cl.commentId FROM CommentLike cl WHERE cl.userId = :userId AND cl.commentId IN :commentIds")
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") Collection<Long> commentIds);

    /**
     * 删除某首歌曲下所有评论的点赞记录。
     * comment_likes 没有外键约束，必须在删除 comments 之前显式清理，否则会产生孤儿数据。
     */
    @Modifying
    @Transactional
    @Query(value = """
            DELETE cl FROM comment_likes cl
            JOIN comments c ON cl.comment_id = c.id
            WHERE c.song_id = :songId
            """, nativeQuery = true)
    int deleteBySongId(@Param("songId") Long songId);
}
