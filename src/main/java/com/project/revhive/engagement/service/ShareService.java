package com.project.revhive.engagement.service;

import com.project.revhive.engagement.model.Share;
import com.project.revhive.engagement.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.project.revhive.engagement.service.integration.NotificationIntegrationService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShareService {

    private final ShareRepository shareRepository;
    private final NotificationIntegrationService notificationIntegrationService;

    @Transactional
    public String sharePost(Long userId, Long postId, String platform) {
        Share share = Share.builder()
                .userId(userId)
                .postId(postId)
                .platform(platform != null ? platform : "INTERNAL")
                .build();

        shareRepository.save(share);
        log.info("Post {} shared by user {} on platform {}", postId, userId, platform);

        // Trigger real-time notification
        try {
            notificationIntegrationService.sendShareNotification(userId, postId);
        } catch (Exception e) {
            log.error("Failed to trigger share notification: {}", e.getMessage());
        }

        return "Post shared successfully";
    }

    @Transactional(readOnly = true)
    public long getShareCount(Long postId) {
        return shareRepository.countByPostId(postId);
    }

    @Transactional(readOnly = true)
    public List<Share> getShares(Long postId) {
        return shareRepository.findByPostId(postId);
    }
}
