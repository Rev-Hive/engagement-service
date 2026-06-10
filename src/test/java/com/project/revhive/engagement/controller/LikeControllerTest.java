package com.project.revhive.engagement.controller;

import com.project.revhive.engagement.exception.DuplicateLikeException;
import com.project.revhive.engagement.exception.GlobalExceptionHandler;
import com.project.revhive.engagement.exception.LikeNotFoundException;
import com.project.revhive.engagement.service.LikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class LikeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LikeService likeService;

    @InjectMocks
    private LikeController likeController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(likeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void testAddLike_Success() throws Exception {
        when(likeService.addLike(1L, 100L)).thenReturn("Liked successfully");

        mockMvc.perform(post("/api/likes")
                        .param("userId", "1")
                        .param("postId", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Liked successfully"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.postId").value(100));
    }

    @Test
    public void testAddLike_Duplicate() throws Exception {
        when(likeService.addLike(1L, 100L)).thenThrow(new DuplicateLikeException("Post already liked by user"));

        mockMvc.perform(post("/api/likes")
                        .param("userId", "1")
                        .param("postId", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Post already liked by user"));
    }

    @Test
    public void testRemoveLike_Success() throws Exception {
        when(likeService.removeLike(1L, 100L)).thenReturn("Unliked successfully");

        mockMvc.perform(delete("/api/likes")
                        .param("userId", "1")
                        .param("postId", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Unliked successfully"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.postId").value(100));
    }

    @Test
    public void testRemoveLike_NotFound() throws Exception {
        when(likeService.removeLike(1L, 100L)).thenThrow(new LikeNotFoundException("Like not found"));

        mockMvc.perform(delete("/api/likes")
                        .param("userId", "1")
                        .param("postId", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Like not found"));
    }

    @Test
    public void testGetLikeCount() throws Exception {
        when(likeService.getLikeCount(100L)).thenReturn(15L);

        mockMvc.perform(get("/api/likes/count")
                        .param("postId", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.postId").value(100))
                .andExpect(jsonPath("$.likeCount").value(15));
    }

    @Test
    public void testIsLiked() throws Exception {
        when(likeService.isLiked(1L, 100L)).thenReturn(true);

        mockMvc.perform(get("/api/likes/check")
                        .param("userId", "1")
                        .param("postId", "100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.isLiked").value(true));
    }

    @Test
    public void testGetLikedPostIds() throws Exception {
        when(likeService.getLikedPostIds(1L)).thenReturn(Arrays.asList(100L, 200L));

        mockMvc.perform(get("/api/likes/user/1/liked-posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(100))
                .andExpect(jsonPath("$[1]").value(200));
    }
}
