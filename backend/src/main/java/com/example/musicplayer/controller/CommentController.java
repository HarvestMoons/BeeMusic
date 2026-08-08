package com.example.musicplayer.controller;

import com.example.musicplayer.dto.CommentDTO;
import com.example.musicplayer.service.CommentService;
import com.example.musicplayer.service.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/public/comments/{songId}")
    public List<CommentDTO> getComments(@PathVariable Long songId,
                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = null;
        if (userDetails != null && userDetails.isEnabled()) {
            userId = userDetails.getUser().getId();
        }
        return commentService.getComments(songId, userId);
    }

    @PostMapping("/comments/add")
    public CommentDTO addComment(@RequestBody Map<String, Object> body,
                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        Long songId = Long.valueOf(body.get("songId").toString());
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        Long replyToUserId = body.get("replyToUserId") != null ? Long.valueOf(body.get("replyToUserId").toString())
                : null;

        return commentService.addComment(userId, songId, content, parentId, replyToUserId);
    }

    @PostMapping("/comments/like/{commentId}")
    public Map<String, Object> likeComment(@PathVariable Long commentId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        return commentService.likeComment(userId, commentId);
    }

    @DeleteMapping("/comments/like/{commentId}")
    public Map<String, Object> unlikeComment(@PathVariable Long commentId,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        return commentService.unlikeComment(userId, commentId);
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(@PathVariable Long commentId,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        commentService.deleteComment(userId, commentId);
    }

    private Long currentUserId(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.isEnabled()) {
            return userDetails.getUser().getId();
        }
        throw new IllegalStateException("未登录");
    }
}
