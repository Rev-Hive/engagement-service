package com.project.revhive.engagement.controller;

import com.project.revhive.engagement.dto.CommentRequest;
import com.project.revhive.engagement.model.Comment;
import com.project.revhive.engagement.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> addComment(@Valid @RequestBody CommentRequest request) {
        log.info("POST /api/comments - Adding comment to post: {}", request.getPostId());
        Comment comment = commentService.addComment(
                request.getUserId(),
                request.getPostId(),
                request.getContent(),
                request.getParentId()
        );
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<Page<Comment>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/comments/post/{} - Page: {}, Size: {}", postId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Comment> comments = commentService.getCommentsByPost(postId, pageable);

        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<Comment>> getRepliesForComment(@PathVariable Long commentId) {
        log.info("GET /api/comments/{}/replies", commentId);
        List<Comment> replies = commentService.getRepliesForComment(commentId);
        return ResponseEntity.ok(replies);
    }

    @GetMapping("/count/{postId}")
    public ResponseEntity<Map<String, Object>> getCommentCount(@PathVariable Long postId) {
        log.info("GET /api/comments/count/{}", postId);

        long count = commentService.getCommentCount(postId);

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("postId", postId);
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {

        log.info("DELETE /api/comments/{} by user: {}", commentId, userId);

        commentService.deleteComment(commentId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Comment deleted successfully");
        response.put("commentId", commentId);
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<Comment> updateComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            @RequestBody String content) {

        log.info("PUT /api/comments/{} by user: {}", commentId, userId);

        Comment updatedComment = commentService.updateComment(commentId, userId, content);

        return ResponseEntity.ok(updatedComment);
    }
}
