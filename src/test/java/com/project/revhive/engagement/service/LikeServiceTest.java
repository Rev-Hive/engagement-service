package com.project.revhive.engagement.service;

import com.project.revhive.engagement.exception.DuplicateLikeException;
import com.project.revhive.engagement.exception.LikeNotFoundException;
import com.project.revhive.engagement.model.Like;
import com.project.revhive.engagement.repository.LikeRepository;
import com.project.revhive.engagement.service.integration.NotificationIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private NotificationIntegrationService notificationIntegrationService;

    @InjectMocks
    private LikeService likeService;

    private Long userId;
    private Long postId;

    @BeforeEach
    public void setUp() {
        userId = 1L;
        postId = 100L;
    }

    @Test
    public void testAddLike_Success() {
        when(likeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        when(likeRepository.saveAndFlush(any(Like.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = likeService.addLike(userId, postId);

        assertEquals("Liked successfully", result);
        verify(likeRepository, times(1)).saveAndFlush(any(Like.class));
        verify(notificationIntegrationService, times(1)).incrementPostLikeCount(postId);
        verify(notificationIntegrationService, times(1)).sendLikeNotification(userId, postId);
    }

    @Test
    public void testAddLike_AlreadyLiked_Check() {
        when(likeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);

        String result = likeService.addLike(userId, postId);

        assertEquals("Already liked", result);
        verify(likeRepository, never()).saveAndFlush(any(Like.class));
        verify(notificationIntegrationService, never()).incrementPostLikeCount(anyLong());
    }

    @Test
    public void testAddLike_DuplicateConstraintViolation() {
        when(likeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        when(likeRepository.saveAndFlush(any(Like.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        assertThrows(DuplicateLikeException.class, () -> {
            likeService.addLike(userId, postId);
        });

        verify(notificationIntegrationService, never()).incrementPostLikeCount(anyLong());
        verify(notificationIntegrationService, never()).sendLikeNotification(anyLong(), anyLong());
    }

    @Test
    public void testRemoveLike_Success() {
        Like like = Like.builder().userId(userId).postId(postId).build();
        when(likeRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(like));

        String result = likeService.removeLike(userId, postId);

        assertEquals("Unliked successfully", result);
        verify(likeRepository, times(1)).delete(like);
        verify(likeRepository, times(1)).flush();
        verify(notificationIntegrationService, times(1)).decrementPostLikeCount(postId);
    }

    @Test
    public void testRemoveLike_NotFound() {
        when(likeRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

        assertThrows(LikeNotFoundException.class, () -> {
            likeService.removeLike(userId, postId);
        });

        verify(likeRepository, never()).delete(any(Like.class));
        verify(notificationIntegrationService, never()).decrementPostLikeCount(anyLong());
    }

    @Test
    public void testGetLikeCount() {
        when(likeRepository.countByPostId(postId)).thenReturn(42L);

        long count = likeService.getLikeCount(postId);

        assertEquals(42L, count);
    }

    @Test
    public void testIsLiked() {
        when(likeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);

        boolean liked = likeService.isLiked(userId, postId);

        assertTrue(liked);
    }

    @Test
    public void testGetLikedPostIds() {
        List<Long> postIds = Arrays.asList(100L, 200L);
        when(likeRepository.findLikedPostIdsByUserId(userId)).thenReturn(postIds);

        List<Long> result = likeService.getLikedPostIds(userId);

        assertEquals(postIds, result);
    }

    @Test
    public void testAddLike_Concurrency() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger duplicateCounter = new AtomicInteger(0);

        when(likeRepository.existsByUserIdAndPostId(anyLong(), anyLong())).thenReturn(false);
        // Simulate database constraint failure for all threads except the first one that successfully saves
        when(likeRepository.saveAndFlush(any(Like.class))).thenAnswer(invocation -> {
            if (successCounter.incrementAndGet() > 1) {
                throw new DataIntegrityViolationException("Duplicate key");
            }
            return invocation.getArgument(0);
        });

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    latch.await();
                    likeService.addLike(userId, postId);
                } catch (DuplicateLikeException e) {
                    duplicateCounter.incrementAndGet();
                } catch (Exception e) {
                    // ignore
                }
            });
        }

        latch.countDown(); // start all threads concurrently
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // One and only one thread should have liked successfully, others must fail with duplicate exception
        assertEquals(1, successCounter.get() - duplicateCounter.get());
        verify(notificationIntegrationService, times(1)).incrementPostLikeCount(postId);
    }
}
