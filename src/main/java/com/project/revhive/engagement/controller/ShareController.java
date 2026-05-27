package com.project.revhive.engagement.controller;

import com.project.revhive.engagement.model.Share;
import com.project.revhive.engagement.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
@Slf4j
public class ShareController {

    private final ShareService shareService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> sharePost(
            @RequestParam Long userId,
            @RequestParam Long postId,
            @RequestParam(required = false) String platform) {

        log.info("POST /api/shares - User {} sharing post {} on platform {}", userId, postId, platform);
        String message = shareService.sharePost(userId, postId, platform);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("userId", userId);
        response.put("postId", postId);
        response.put("platform", platform != null ? platform : "INTERNAL");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getShareCount(@RequestParam Long postId) {
        log.info("GET /api/shares/count - Post {}", postId);
        long count = shareService.getShareCount(postId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("postId", postId);
        response.put("shareCount", count);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Share>> getShares(@RequestParam Long postId) {
        log.info("GET /api/shares - Post {}", postId);
        List<Share> shares = shareService.getShares(postId);
        return ResponseEntity.ok(shares);
    }
}
