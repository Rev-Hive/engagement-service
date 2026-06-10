package com.project.revhive.engagement.service;

import com.project.revhive.engagement.model.Like;
import com.project.revhive.engagement.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.revhive.engagement.service.integration.NotificationIntegrationService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LikeService {

    private final LikeRepository likeRepository;
    private final NotificationIntegrationService notificationIntegrationService;

    @Transactional
    public String addLike(Long userId, Long postId) {
        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            log.info("Check exists: Post {} already liked by user {}", postId, userId);
            return "Already liked";
        }

        try {
            Like like = Like.builder()
                    .userId(userId)
                    .postId(postId)
                    .build();

            // Force immediate DB write to trigger unique constraint check before making external REST calls
            likeRepository.saveAndFlush(like);
            log.info("Post {} liked by user {} and flushed to DB", postId, userId);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Duplicate like detected via DB unique constraint for postId: {}, userId: {}", postId, userId);
            throw new com.project.revhive.engagement.exception.DuplicateLikeException("Post already liked by user");
        }

        // Update post-service like count
        try {
            notificationIntegrationService.incrementPostLikeCount(postId);
        } catch (Exception e) {
            log.error("Failed to increment post like count in post-service: {}", e.getMessage());
        }

        // Trigger real-time notification
        try {
            notificationIntegrationService.sendLikeNotification(userId, postId);
        } catch (Exception e) {
            log.error("Failed to trigger like notification: {}", e.getMessage());
        }

        return "Liked successfully";
    }

    @Transactional
    public String removeLike(Long userId, Long postId) {
        Like like = likeRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new com.project.revhive.engagement.exception.LikeNotFoundException("Like not found"));

        likeRepository.delete(like);
        // Force immediate DB write to verify deletion before making external REST calls
        likeRepository.flush();
        log.info("Post {} unliked by user {} and flushed to DB", postId, userId);

        // Update post-service like count
        try {
            notificationIntegrationService.decrementPostLikeCount(postId);
        } catch (Exception e) {
            log.error("Failed to decrement post like count in post-service: {}", e.getMessage());
        }

        return "Unliked successfully";
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, Long postId) {
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Transactional(readOnly = true)
    public java.util.List<Long> getLikedPostIds(Long userId) {
        return likeRepository.findLikedPostIdsByUserId(userId);
    }
}
