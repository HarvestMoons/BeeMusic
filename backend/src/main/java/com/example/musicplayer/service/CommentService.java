package com.example.musicplayer.service;

import com.example.musicplayer.dto.CommentDTO;
import com.example.musicplayer.model.Comment;
import com.example.musicplayer.model.User;
import com.example.musicplayer.repository.CommentLikeRepository;
import com.example.musicplayer.repository.CommentRepository;
import com.example.musicplayer.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, CommentLikeRepository commentLikeRepository,
            UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
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
        commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        int inserted = commentLikeRepository.insertIfAbsent(userId, commentId);
        if (inserted > 0) {
            commentRepository.incrementLikeCount(commentId);
        }
        return buildCommentLikeState(commentId, true);
    }

    @Transactional
    public Map<String, Object> unlikeComment(Long userId, Long commentId) {
        int deleted = commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
        if (deleted > 0) {
            commentRepository.decrementLikeCount(commentId);
        }
        return buildCommentLikeState(commentId, false);
    }

    private Map<String, Object> buildCommentLikeState(Long commentId, boolean liked) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        int likeCount = comment.getLikeCount() == null ? 0 : Math.max(comment.getLikeCount(), 0);
        return Map.of(
                "commentId", commentId,
                "liked", liked,
                "likeCount", likeCount);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Optional<Comment> opt = commentRepository.findById(commentId);
        if (opt.isPresent()) {
            Comment c = opt.get();
            // 只有作者能删
            if (c.getUserId().equals(userId)) {
                c.setIsDeleted(1);
                commentRepository.save(c);
            }
        }
    }
}
