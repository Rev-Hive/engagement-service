package com.project.revhive.engagement.dto;

import lombok.Data;

@Data
public class CommentResponse {
    private Long id;
    private Long userId;
    private String username;
    private String content;
}
