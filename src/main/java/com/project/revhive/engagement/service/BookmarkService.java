package com.project.revhive.engagement.service;

import com.project.revhive.engagement.model.Bookmark;
import com.project.revhive.engagement.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public String addBookmark(Long userId, Long postId) {
        if (bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
            return "Already bookmarked";
        }

        Bookmark bookmark = Bookmark.builder()
                .userId(userId)
                .postId(postId)
                .build();

        bookmarkRepository.save(bookmark);
        log.info("Post {} bookmarked by user {}", postId, userId);
        return "Bookmarked successfully";
    }

    @Transactional
    public String removeBookmark(Long userId, Long postId) {
        Bookmark bookmark = bookmarkRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new RuntimeException("Bookmark not found"));

        bookmarkRepository.delete(bookmark);
        log.info("Post {} bookmark removed by user {}", postId, userId);
        return "Bookmark removed";
    }

    @Transactional(readOnly = true)
    public List<Bookmark> getBookmarks(Long userId) {
        return bookmarkRepository.findByUserId(userId);
    }
}
