package com.example.musicplayer.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDTO {
    private Long id;
    private Long songId;
    private Long userId;
    private String username;
    private String content;
    private Long parentId;
    private Long replyToUserId;
    private String replyToUsername;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private boolean isLiked; // 当前用户是否点赞
    private boolean isOwner; // 当前用户是否是评论作者

    private List<CommentDTO> replies = new ArrayList<>();
}
