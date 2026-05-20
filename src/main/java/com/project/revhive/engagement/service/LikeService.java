package com.project.revhive.engagement.service;

import com.project.revhive.engagement.model.Like;
import com.project.revhive.engagement.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional
    public String addLike(Long userId, Long postId) {
        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            return "Already liked";
        }

        Like like = Like.builder()
                .userId(userId)
                .postId(postId)
                .build();

        likeRepository.save(like);
        log.info("Post {} liked by user {}", postId, userId);
        return "Liked successfully";
    }

    @Transactional
    public String removeLike(Long userId, Long postId) {
        Like like = likeRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new RuntimeException("Like not found"));

        likeRepository.delete(like);
        log.info("Post {} unliked by user {}", postId, userId);
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
}
