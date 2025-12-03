package com.example.musicplayer.controller;

import com.example.musicplayer.dto.CommentDTO;
import com.example.musicplayer.model.User;
import com.example.musicplayer.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
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
    public List<CommentDTO> getComments(@PathVariable Long songId, HttpServletRequest request) {
        Long userId = null;
        Object userObj = request.getSession().getAttribute("user");
        if (userObj instanceof User u) {
            userId = u.getId();
        }
        return commentService.getComments(songId, userId);
    }

    @PostMapping("/comments/add")
    public CommentDTO addComment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = currentUserId(request);
        Long songId = Long.valueOf(body.get("songId").toString());
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        Long replyToUserId = body.get("replyToUserId") != null ? Long.valueOf(body.get("replyToUserId").toString()) : null;

        return commentService.addComment(userId, songId, content, parentId, replyToUserId);
    }

    @PostMapping("/comments/like/{commentId}")
    public void likeComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        commentService.likeComment(userId, commentId);
    }

    @DeleteMapping("/comments/like/{commentId}")
    public void unlikeComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        commentService.unlikeComment(userId, commentId);
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(@PathVariable Long commentId, HttpServletRequest request) {
        Long userId = currentUserId(request);
        commentService.deleteComment(userId, commentId);
    }

    private Long currentUserId(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute("user");
        if (userObj instanceof User u) {
            return u.getId();
        }
        throw new IllegalStateException("未登录");
    }
}
