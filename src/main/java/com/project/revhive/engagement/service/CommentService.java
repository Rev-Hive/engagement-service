package com.project.revhive.engagement.service;

import com.project.revhive.engagement.model.Comment;
import com.project.revhive.engagement.repository.CommentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import com.project.revhive.engagement.service.integration.NotificationIntegrationService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final NotificationIntegrationService notificationIntegrationService;

    public Comment addComment(Long postId, String content, Long parentId,String username,Long userId) {

        Comment.CommentBuilder commentBuilder = Comment.builder()
                .postId(postId)
                .content(content)
                .userId(userId)
                .username(username)
                .isActive(true)
                .likeCount(0);

        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId)
                    .orElseThrow(() ->
                            new RuntimeException("Parent comment not found with id: " + parentId));

            if (!parentComment.getPostId().equals(postId)) {
                throw new RuntimeException("Parent comment does not belong to the same post");
            }

            commentBuilder.parent(parentComment);
        }

        Comment savedComment = commentRepository.save(commentBuilder.build());

        // Trigger real-time notification
        try {
            notificationIntegrationService.sendCommentNotification(userId, username, postId, parentId, content);
        } catch (Exception e) {
            log.error("Failed to trigger comment notification: {}", e.getMessage());
        }

        return savedComment;
    }

    public Page<Comment> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostIdAndParentIsNullAndIsActiveTrue(
                postId,
                pageable
        );
    }

    public List<Comment> getRepliesForComment(Long commentId) {
        return commentRepository.findByParentIdAndIsActiveTrue(commentId);
    }

    public long getCommentCount(Long postId) {
        return commentRepository.countByPostIdAndIsActiveTrue(postId);
    }

    public void deleteComment(Long commentId, Long userId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own comments");
        }

        commentRepository.softDeleteComment(commentId);
    }

    public Comment updateComment(Long commentId, Long userId, String content) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only edit your own comments");
        }

        comment.setContent(content);

        return commentRepository.save(comment);
    }
}
