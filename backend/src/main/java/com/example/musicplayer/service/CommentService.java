package com.example.musicplayer.service;

import com.example.musicplayer.dto.CommentDTO;
import com.example.musicplayer.model.Comment;
import com.example.musicplayer.model.CommentLike;
import com.example.musicplayer.model.User;
import com.example.musicplayer.repository.CommentLikeRepository;
import com.example.musicplayer.repository.CommentRepository;
import com.example.musicplayer.repository.UserRepository;
import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.exception.UserOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;

    public CommentService(CommentRepository commentRepository, CommentLikeRepository commentLikeRepository,
            UserRepository userRepository, SongRepository songRepository) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
        this.songRepository = songRepository;
    }

    public List<CommentDTO> getComments(Long songId, Long currentUserId) {
        List<Comment> allComments = commentRepository.findBySongIdAndIsDeletedOrderByLikeCountDescCreatedAtDesc(songId,
                0);
        if (allComments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = new HashSet<>();
        for (Comment c : allComments) {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) {
                userIds.add(c.getReplyToUserId());
            }
        }
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> likedCommentIds = Collections.emptySet();
        if (currentUserId != null) {
            List<Long> commentIds = allComments.stream()
                    .map(Comment::getId)
                    .collect(Collectors.toList());
            if (!commentIds.isEmpty()) {
                likedCommentIds = new HashSet<>(commentLikeRepository.findLikedCommentIds(currentUserId, commentIds));
            }
        }

        List<CommentDTO> rootComments = new ArrayList<>();
        Map<Long, CommentDTO> dtoMap = new HashMap<>();

        // 先转 DTO
        List<CommentDTO> allDTOs = new ArrayList<>();
        for (Comment c : allComments) {
            CommentDTO dto = new CommentDTO();
            dto.setId(c.getId());
            dto.setSongId(c.getSongId());
            dto.setUserId(c.getUserId());

            User u = userMap.get(c.getUserId());
            if (u != null) {
                dto.setUsername(u.getUsername());
                dto.setUserRole(u.getRoleEnum().name());
            } else {
                dto.setUsername("Unknown");
                dto.setUserRole("USER");
            }

            dto.setContent(c.getContent());
            dto.setParentId(c.getParentId());
            dto.setReplyToUserId(c.getReplyToUserId());
            if (c.getReplyToUserId() != null) {
                User replyUser = userMap.get(c.getReplyToUserId());
                dto.setReplyToUsername(replyUser != null ? replyUser.getUsername() : "Unknown");
            }
            dto.setLikeCount(c.getLikeCount());
            dto.setCreatedAt(c.getCreatedAt());

            if (currentUserId != null) {
                dto.setLiked(likedCommentIds.contains(c.getId()));
                dto.setOwner(currentUserId.equals(c.getUserId()));
            }

            allDTOs.add(dto);
            dtoMap.put(dto.getId(), dto);
        }

        // 组装两层评论树
        // - parentId 为 null  → 根评论
        // - parentId 不为 null → 直接挂到 dtoMap.get(parentId) 这条评论的 replies 下
        //
        // 注意：这里不做"上溯到根评论"的扁平化处理。两层结构依赖前端约定维持：
        // 回复二级评论时 parentId 传根评论 ID、replyToUserId 传被回复用户的 ID。
        // 若传入的 parentId 指向的是二级评论，该回复会被挂到那条二级评论下（形成三层），不会被提升到根评论。
        for (CommentDTO dto : allDTOs) {
            if (dto.getParentId() == null) {
                rootComments.add(dto);
            }
        }

        for (CommentDTO dto : allDTOs) {
            if (dto.getParentId() != null) {
                CommentDTO parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    parent.getReplies().add(dto);
                }
            }
        }

        // 对子评论排序 (按时间正序，楼层效果)
        for (CommentDTO root : rootComments) {
            root.getReplies().sort(Comparator.comparing(CommentDTO::getCreatedAt));
        }

        return rootComments;
    }

    @Transactional
    public CommentDTO addComment(Long userId, Long songId, String content, Long parentId, Long replyToUserId) {
        if (songId == null || songId <= 0) throw new UserOperationException("歌曲 ID 不正确");
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isBlank() || trimmed.length() > 1000) {
            throw new UserOperationException("评论内容需为 1 到 1000 个字符");
        }
        var song = songRepository.findLockedById(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "歌曲不存在"));
        if (song.getIsDeleted() != 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "歌曲已删除");
        if (parentId != null) {
            Comment parent = activeComment(parentId);
            if (!songId.equals(parent.getSongId()) || parent.getParentId() != null) {
                throw new UserOperationException("父评论必须是当前歌曲的根评论");
            }
            if (replyToUserId == null) replyToUserId = parent.getUserId();
            if (!userRepository.existsById(replyToUserId)) throw new UserOperationException("被回复用户不存在");
        } else if (replyToUserId != null) {
            throw new UserOperationException("回复用户必须同时指定父评论");
        }
        content = trimmed;
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setSongId(songId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setReplyToUserId(replyToUserId);

        comment = commentRepository.save(comment);

        // 构造返回 DTO
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setSongId(songId);
        dto.setUserId(userId);

        User currentUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        dto.setUsername(currentUser.getUsername());
        dto.setUserRole(currentUser.getRoleEnum().name());

        dto.setContent(content);
        dto.setParentId(parentId);
        dto.setReplyToUserId(replyToUserId);
        if (replyToUserId != null) {
            dto.setReplyToUsername(userRepository.findById(replyToUserId).map(User::getUsername).orElse("Unknown"));
        }
        dto.setLikeCount(0);
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setLiked(false);
        dto.setOwner(true);
        return dto;
    }

    @Transactional
    public Map<String, Object> likeComment(Long userId, Long commentId) {
        activeComment(commentId);
        if (!commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            commentLikeRepository.save(like);
            commentRepository.incrementLikeCount(commentId);
        }
        return buildCommentLikeState(commentId, true);
    }

    @Transactional
    public Map<String, Object> unlikeComment(Long userId, Long commentId) {
        activeComment(commentId);
        int deleted = commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
        if (deleted > 0) {
            commentRepository.decrementLikeCount(commentId);
        }
        return buildCommentLikeState(commentId, false);
    }

    private Map<String, Object> buildCommentLikeState(Long commentId, boolean liked) {
        Integer count = commentRepository.findLikeCount(commentId);
        int likeCount = count == null ? 0 : Math.max(count, 0);
        return Map.of(
                "commentId", commentId,
                "liked", liked,
                "likeCount", likeCount);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = activeComment(commentId);
        if (!comment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能删除自己的评论");
        }
        comment.setIsDeleted(1);
        commentRepository.save(comment);
    }

    private Comment activeComment(Long commentId) {
        if (commentId == null || commentId <= 0) throw new UserOperationException("评论 ID 不正确");
        Comment comment = commentRepository.findLockedById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在"));
        if (comment.getIsDeleted() != 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论已删除");
        return comment;
    }
}
