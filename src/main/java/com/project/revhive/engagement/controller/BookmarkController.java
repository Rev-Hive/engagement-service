package com.project.revhive.engagement.controller;

import com.project.revhive.engagement.model.Bookmark;
import com.project.revhive.engagement.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> addBookmark(
            @RequestParam Long userId,
            @RequestParam Long postId) {

        log.info("POST /api/bookmarks - User {} bookmarking post {}", userId, postId);
        String message = bookmarkService.addBookmark(userId, postId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("userId", userId);
        response.put("postId", postId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> removeBookmark(
            @RequestParam Long userId,
            @RequestParam Long postId) {

        log.info("DELETE /api/bookmarks - User {} removing bookmark on post {}", userId, postId);
        String message = bookmarkService.removeBookmark(userId, postId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("userId", userId);
        response.put("postId", postId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Bookmark>> getBookmarks(@PathVariable Long userId) {
        log.info("GET /api/bookmarks/user/{}", userId);
        List<Bookmark> bookmarks = bookmarkService.getBookmarks(userId);
        return ResponseEntity.ok(bookmarks);
    }
}
