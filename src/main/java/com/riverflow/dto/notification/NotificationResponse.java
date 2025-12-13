package com.riverflow.dto.notification;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String message;
    private String entityType;
    private String entityId;
    private String actionUrl;
    private String actionLabel;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
