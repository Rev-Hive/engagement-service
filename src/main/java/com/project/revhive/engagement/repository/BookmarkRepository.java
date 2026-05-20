package com.project.revhive.engagement.repository;

import com.project.revhive.engagement.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Optional<Bookmark> findByUserIdAndPostId(Long userId, Long postId);

    List<Bookmark> findByUserId(Long userId);

    void deleteByUserIdAndPostId(Long userId, Long postId);
}
