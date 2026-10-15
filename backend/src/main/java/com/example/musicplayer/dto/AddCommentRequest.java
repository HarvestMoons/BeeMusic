package com.example.musicplayer.dto;

import lombok.Data;

@Data
public class AddCommentRequest {
    private Long songId;
    private String content;
    private Long parentId;
    private Long replyToUserId;
}
