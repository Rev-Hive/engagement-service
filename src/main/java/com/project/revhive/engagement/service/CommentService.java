package com.project.revhive.engagement.service;

import com.project.revhive.engagement.model.Comment;
import com.project.revhive.engagement.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

    @Transactional
    public Comment addComment(Long userId, Long postId, String content, Long parentId) {
        log.info("Adding comment to post: {} by user: {} (Parent: {})", postId, userId, parentId);

        Comment.CommentBuilder commentBuilder = Comment.builder()
                .userId(userId)
                .postId(postId)
                .content(content)
                .isActive(true)
                .likeCount(0);

        if (parentId != null) {
            Comment parentComment = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found with id: " + parentId));
            
            if (!parentComment.getPostId().equals(postId)) {
                throw new RuntimeException("Parent comment does not belong to the same post");
            }
            
            commentBuilder.parent(parentComment);
        }

        Comment savedComment = commentRepository.save(commentBuilder.build());
        log.info("Comment (ID: {}) added successfully", savedComment.getId());
        return savedComment;
    }

    @Transactional(readOnly = true)
    public Page<Comment> getCommentsByPost(Long postId, Pageable pageable) {
        log.info("Fetching top-level comments for post: {}", postId);
        return commentRepository.findByPostIdAndParentIsNullAndIsActiveTrue(postId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Comment> getRepliesForComment(Long commentId) {
        log.info("Fetching replies for comment: {}", commentId);
        return commentRepository.findByParentIdAndIsActiveTrue(commentId);
    }

    @Transactional(readOnly = true)
    public long getCommentCount(Long postId) {
        log.info("Getting comment count for post: {}", postId);
        return commentRepository.countByPostIdAndIsActiveTrue(postId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.info("Deleting comment: {} by user: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own comments");
        }

        commentRepository.softDeleteComment(commentId);
        log.info("Comment deleted successfully");
    }

    @Transactional
    public Comment updateComment(Long commentId, Long userId, String content) {
        log.info("Updating comment: {} by user: {}", commentId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only edit your own comments");
        }

        comment.setContent(content);
        Comment updatedComment = commentRepository.save(comment);

        log.info("Comment updated successfully");
        return updatedComment;
    }
}
