package com.project.revhive.engagement.service.integration;

import com.project.revhive.engagement.dto.NotificationRequest;
import com.project.revhive.engagement.dto.PostDto;
import com.project.revhive.engagement.dto.PostServiceResponse;
import com.project.revhive.engagement.dto.UserProfileDto;
import com.project.revhive.engagement.model.Comment;
import com.project.revhive.engagement.repository.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@Service
@Slf4j
public class NotificationIntegrationService {

    private final RestTemplate restTemplate;
    private final CommentRepository commentRepository;
    private final String userServiceUrl;
    private final String postServiceUrl;
    private final String notificationServiceUrl;

    public NotificationIntegrationService(
            RestTemplate restTemplate,
            CommentRepository commentRepository,
            @Value("${app.services.user-service}") String userServiceUrl,
            @Value("${app.services.post-service}") String postServiceUrl,
            @Value("${app.services.notification-service}") String notificationServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.commentRepository = commentRepository;
        this.userServiceUrl = userServiceUrl;
        this.postServiceUrl = postServiceUrl;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    private String getCorrelationId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("X-Correlation-Id");
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    // Helper to generate dynamic authorization headers propagating the active JWT token and correlation ID
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                if (authHeader != null && !authHeader.isEmpty()) {
                    headers.set("Authorization", authHeader);
                }
                String correlationId = attributes.getRequest().getHeader("X-Correlation-Id");
                if (correlationId != null && !correlationId.isEmpty()) {
                    headers.set("X-Correlation-Id", correlationId);
                }
            }
        } catch (Exception ex) {
            log.warn("Could not propagate JWT token/CorrelationId inside engagement-service: {}", ex.getMessage());
        }
        return headers;
    }

    // Resolve post details with JWT token authorization propagated
    private PostDto getPost(Long postId) {
        String correlationId = getCorrelationId();
        try {
            String url = postServiceUrl + "/api/posts/" + postId;
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<PostServiceResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    PostServiceResponse.class
            );
            PostServiceResponse body = response.getBody();
            if (body != null && body.isSuccess() && body.getData() != null) {
                return body.getData();
            }
        } catch (Exception e) {
            log.warn("[CorrelationId: {}] Failed to fetch post {} from post-service (URL: {}), falling back. Error: {}", correlationId, postId, postServiceUrl, e.getMessage());
        }
        return null;
    }

    // Resolve user's username with JWT token authorization propagated
    private String getUsername(Long userId) {
        String correlationId = getCorrelationId();
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<UserProfileDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserProfileDto.class
            );
            UserProfileDto profile = response.getBody();
            if (profile != null && profile.getUsername() != null) {
                return profile.getUsername();
            }
        } catch (Exception e) {
            log.warn("[CorrelationId: {}] Failed to fetch username for user {} from user-service (URL: {}), falling back. Error: {}", correlationId, userId, userServiceUrl, e.getMessage());
        }
        return "Someone";
    }

    // Helper to send a notification request to notification-service with JWT token authorization propagated and retries
    private void send(NotificationRequest request) {
        String correlationId = getCorrelationId();
        int maxRetries = 3;
        int attempt = 0;
        long backoffMs = 500;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                attempt++;
                String notifUrl = notificationServiceUrl + "/api/notifications";
                HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, getHeaders());
                restTemplate.exchange(notifUrl, HttpMethod.POST, entity, Object.class);
                log.info("[CorrelationId: {}] Successfully sent notification to notification-service for user {} on attempt {}", 
                        correlationId, request.getUserId(), attempt);
                return; // success, return
            } catch (Exception e) {
                lastException = e;
                log.warn("[CorrelationId: {}] Attempt {} to send notification to notification-service failed: {}", 
                        correlationId, attempt, e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(backoffMs);
                        backoffMs *= 2; // exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("[CorrelationId: {}] Notification retry backoff interrupted", correlationId);
                        break;
                    }
                }
            }
        }
        log.error("[CorrelationId: {}] Failed to post notification to notification-service after {} attempts. Error: {}", 
                correlationId, maxRetries, lastException.getMessage(), lastException);
        throw new RuntimeException("Failed to deliver notification after retries", lastException);
    }

    // Truncate message contents to fit nicely in notifications
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    // 1. Post Like Notification
    public void sendLikeNotification(Long likerId, Long postId) {
        try {
            PostDto post = getPost(postId);
            if (post == null) {
                log.warn("Post not found; skipping like notification.");
                return;
            }

            Long recipientId = post.getUserId();
            // Suppress self-notification
            if (likerId.equals(recipientId)) {
                log.info("Liker is the post author; suppressing self-notification.");
                return;
            }

            String likerUsername = getUsername(likerId);
            String resolvedLiker = likerUsername != null ? likerUsername : "Someone";
            String messageText = "Someone".equals(resolvedLiker)
                    ? "Someone liked your post!"
                    : "@" + resolvedLiker + " liked your post!";

            NotificationRequest request = NotificationRequest.builder()
                    .userId(recipientId)
                    .title("New Like")
                    .message(messageText)
                    .type("LIKE")
                    .build();

            send(request);

        } catch (Exception e) {
            log.error("Failed to process like notification: {}", e.getMessage(), e);
        }
    }

    // 2. Post Comment Notification (or Reply Notification)
    public void sendCommentNotification(Long commenterId, String commenterUsername, Long postId, Long parentId, String content) {
        try {
            // Resolve the commenter username dynamically from user-service if the request header was missing or null
            String resolvedCommenter = (commenterUsername != null && !commenterUsername.trim().isEmpty())
                    ? commenterUsername
                    : getUsername(commenterId);

            if (resolvedCommenter == null) {
                resolvedCommenter = "Someone";
            }

            // Check if it is a reply to another comment
            if (parentId != null) {
                Comment parentComment = commentRepository.findById(parentId).orElse(null);
                if (parentComment != null) {
                    Long recipientId = parentComment.getUserId();
                    // Suppress self-reply notification
                    if (commenterId.equals(recipientId)) {
                        log.info("User replied to their own comment; suppressing reply notification.");
                    } else {
                        String truncatedReply = truncate(content, 60);
                        String commenterHandle = "Someone".equals(resolvedCommenter) ? "Someone" : "@" + resolvedCommenter;
                        NotificationRequest request = NotificationRequest.builder()
                                .userId(recipientId)
                                .title("New Reply")
                                .message(commenterHandle + " replied to your comment: \"" + truncatedReply + "\"")
                                .type("COMMENT_REPLY")
                                .build();
                        send(request);
                    }
                }
            }

            // Always notify post author about the comment (unless commenter is the post author)
            PostDto post = getPost(postId);
            if (post != null) {
                Long postAuthorId = post.getUserId();
                // Suppress self-notification
                if (commenterId.equals(postAuthorId)) {
                    log.info("Commenter is the post author; suppressing post comment notification.");
                    return;
                }

                // If it is a top-level comment (not a reply), notify the post author
                if (parentId == null) {
                    String truncatedComment = truncate(content, 60);
                    String commenterHandle = "Someone".equals(resolvedCommenter) ? "Someone" : "@" + resolvedCommenter;
                    NotificationRequest request = NotificationRequest.builder()
                            .userId(postAuthorId)
                            .title("New Comment")
                            .message(commenterHandle + " commented on your post: \"" + truncatedComment + "\"")
                            .type("COMMENT")
                            .build();
                    send(request);
                }
            }

        } catch (Exception e) {
            log.error("Failed to process comment/reply notification: {}", e.getMessage(), e);
        }
    }

    // 3. Post Share Notification
    public void sendShareNotification(Long sharerId, Long postId) {
        try {
            PostDto post = getPost(postId);
            if (post == null) {
                log.warn("Post not found; skipping share notification.");
                return;
            }

            Long recipientId = post.getUserId();
            // Suppress self-notification
            if (sharerId.equals(recipientId)) {
                log.info("Sharer is the post author; suppressing self-notification.");
                return;
            }

            String sharerUsername = getUsername(sharerId);
            String resolvedSharer = sharerUsername != null ? sharerUsername : "Someone";
            String messageText = "Someone".equals(resolvedSharer)
                    ? "Someone shared your post!"
                    : "@" + resolvedSharer + " shared your post!";

            NotificationRequest request = NotificationRequest.builder()
                    .userId(recipientId)
                    .title("New Share")
                    .message(messageText)
                    .type("SHARE")
                    .build();

            send(request);

        } catch (Exception e) {
            log.error("Failed to process share notification: {}", e.getMessage(), e);
        }
    }

    public void incrementPostLikeCount(Long postId) {
        try {
            String url = postServiceUrl + "/api/posts/" + postId + "/like";
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Object.class
            );
            log.info("Successfully called post-service to increment like count for post {}", postId);
        } catch (Exception e) {
            log.error("Failed to increment like count in post-service for post {}: {}", postId, e.getMessage());
        }
    }

    public void decrementPostLikeCount(Long postId) {
        try {
            String url = postServiceUrl + "/api/posts/" + postId + "/like";
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Object.class
            );
            log.info("Successfully called post-service to decrement like count for post {}", postId);
        } catch (Exception e) {
            log.error("Failed to decrement like count in post-service for post {}: {}", postId, e.getMessage());
        }
    }
}
