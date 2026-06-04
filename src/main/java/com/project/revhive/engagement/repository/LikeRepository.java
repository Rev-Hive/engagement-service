package com.project.revhive.engagement.repository;

import com.project.revhive.engagement.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    long countByPostId(Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    @org.springframework.data.jpa.repository.Query("SELECT l.postId FROM Like l WHERE l.userId = :userId")
    java.util.List<Long> findLikedPostIdsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
