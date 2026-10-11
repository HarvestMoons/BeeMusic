package com.example.musicplayer.repository;

import com.example.musicplayer.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findBySongIdAndIsDeletedOrderByLikeCountDescCreatedAtDesc(Long songId, int isDeleted);

    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    /**
     * 删除某首歌曲的全部评论（含已软删除的）。
     * comments.song_id 在数据库中没有外键，不会级联，必须显式清理。
     * comments.parent_id 同样没有外键约束，因此单条批量 DELETE 即可，无需按父子顺序拆分。
     * 调用前必须先删除 comment_likes。
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.songId = :songId")
    int deleteBySongId(@Param("songId") Long songId);
}
