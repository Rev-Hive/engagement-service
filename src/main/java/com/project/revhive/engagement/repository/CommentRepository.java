package com.project.revhive.engagement.repository;

import com.project.revhive.engagement.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndParentIsNullAndIsActiveTrue(
            Long postId,
            Pageable pageable
    );

    List<Comment> findByParentIdAndIsActiveTrue(Long parentId);

    long countByPostIdAndIsActiveTrue(Long postId);

    @Modifying
    @Query("UPDATE Comment c SET c.isActive = false WHERE c.id = :commentId")
    void softDeleteComment(@Param("commentId") Long commentId);
}
