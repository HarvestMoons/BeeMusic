package com.example.musicplayer.service;

import com.example.musicplayer.dto.CommentDTO;
import com.example.musicplayer.model.Comment;
import com.example.musicplayer.model.CommentLike;
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

    public CommentService(CommentRepository commentRepository, CommentLikeRepository commentLikeRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.userRepository = userRepository;
    }

    public List<CommentDTO> getComments(Long songId, Long currentUserId) {
        List<Comment> allComments = commentRepository.findBySongIdAndIsDeletedOrderByLikeCountDescCreatedAtDesc(songId, 0);
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

        // 组装树 (两层结构)
        // 规则：parentId 为 null 的是根评论。
        // parentId 不为 null 的，找到其 parent (必须是根评论)。
        // 如果 parentId 指向的是二级评论，则将其归类到该二级评论所属的根评论下（扁平化二级）。
        // 但目前的数据库设计 parentId 指向的是直接父级。
        // 需求是“两层结构”。
        // 策略：如果 c.parentId 是根评论，直接加。
        // 如果 c.parentId 是子评论，则需要找到最顶层的根。
        // 为了简化，通常前端传参时，如果是回复子评论，parentId 传根评论ID，replyToUserId 传被回复人ID。
        // 这样数据库里 parentId 永远是根评论ID。
        // 让我们约定：前端在回复子评论时，parentId 填该子评论的 parentId (即根ID)。

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
    public void likeComment(Long userId, Long commentId) {
        if (!commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            commentLikeRepository.save(like);
            commentRepository.incrementLikeCount(commentId);
        }
    }

    @Transactional
    public void unlikeComment(Long userId, Long commentId) {
        Optional<CommentLike> like = commentLikeRepository.findByUserIdAndCommentId(userId, commentId);
        if (like.isPresent()) {
            commentLikeRepository.delete(like.get());
            commentRepository.decrementLikeCount(commentId);
        }
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
