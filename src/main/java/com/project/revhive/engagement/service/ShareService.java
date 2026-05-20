package com.project.revhive.engagement.service;

import com.project.revhive.engagement.model.Share;
import com.project.revhive.engagement.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShareService {

    private final ShareRepository shareRepository;

    @Transactional
    public String sharePost(Long userId, Long postId, String platform) {
        Share share = Share.builder()
                .userId(userId)
                .postId(postId)
                .platform(platform != null ? platform : "INTERNAL")
                .build();

        shareRepository.save(share);
        log.info("Post {} shared by user {} on platform {}", postId, userId, platform);
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
