package com.project.revhive.engagement.repository;

import com.project.revhive.engagement.model.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

    long countByPostId(Long postId);

    List<Share> findByPostId(Long postId);
}
