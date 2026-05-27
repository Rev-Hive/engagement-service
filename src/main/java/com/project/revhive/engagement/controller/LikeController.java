package com.project.revhive.engagement.controller;

import com.project.revhive.engagement.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Slf4j
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> addLike(
            @RequestParam Long userId,
            @RequestParam Long postId) {

        log.info("POST /api/likes - User {} liking post {}", userId, postId);
        String message = likeService.addLike(userId, postId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("userId", userId);
        response.put("postId", postId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> removeLike(
            @RequestParam Long userId,
            @RequestParam Long postId) {

        log.info("DELETE /api/likes - User {} unliking post {}", userId, postId);
        String message = likeService.removeLike(userId, postId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("userId", userId);
        response.put("postId", postId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getLikeCount(@RequestParam Long postId) {
        log.info("GET /api/likes/count - Post {}", postId);
        long count = likeService.getLikeCount(postId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("postId", postId);
        response.put("likeCount", count);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> isLiked(
            @RequestParam Long userId,
            @RequestParam Long postId) {

        log.info("GET /api/likes/check - User {} liked post {}", userId, postId);
        boolean liked = likeService.isLiked(userId, postId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", userId);
        response.put("postId", postId);
        response.put("isLiked", liked);

        return ResponseEntity.ok(response);
    }
}
