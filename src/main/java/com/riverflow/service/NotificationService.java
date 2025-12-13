package com.riverflow.service;

import com.riverflow.dto.notification.NotificationResponse;
import com.riverflow.model.Notification;
import com.riverflow.model.User;
import com.riverflow.repository.NotificationRepository;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Notification type constants
     */
    public static final String TYPE_PROJECT_LEFT = "PROJECT_LEFT";
    public static final String TYPE_PROJECT_INVITE = "PROJECT_INVITE";
    public static final String TYPE_CREDIT_TOPUP_SUCCESS = "CREDIT_TOPUP_SUCCESS";
    public static final String TYPE_PROJECT_REMOVED = "PROJECT_REMOVED";
    public static final String TYPE_TICKET_RESPONSE = "TICKET_RESPONSE";
    public static final String TYPE_TICKET_UPDATE = "TICKET_UPDATE";

    /**
     * Create a new notification for a user
     */
    @Transactional
    public Notification createNotification(
            Long userId,
            String type,
            String title,
            String message,
            String entityType,
            String entityId,
            String actionUrl,
            String actionLabel) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .actionUrl(actionUrl)
                .actionLabel(actionLabel)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification [{}] for user {}: {}", type, userId, title);
        return saved;
    }

    /**
     * Create a simple notification without entity reference
     */
    @Transactional
    public Notification createNotification(Long userId, String type, String title, String message) {
        return createNotification(userId, type, title, message, null, null, null, null);
    }

    /**
     * Get all notifications for a user
     */
    public List<NotificationResponse> getUserNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get unread notification count for a user
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        // Verify ownership
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return mapToResponse(notification);
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked all notifications as read for user {}", userId);
    }

    /**
     * Map entity to response DTO
     */
    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .actionUrl(notification.getActionUrl())
                .actionLabel(notification.getActionLabel())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
