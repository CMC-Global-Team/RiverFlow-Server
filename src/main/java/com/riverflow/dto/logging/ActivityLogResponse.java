package com.riverflow.dto.logging;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for activity logs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponse {

    private String id;
    private Long actorId;
    private String actorEmail;
    private String actorRole;
    private String action;
    private String category;
    private String targetId;
    private String targetType;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;

    /**
     * Formatted timestamp for display
     */
    private String formattedTimestamp;

    /**
     * Display-friendly action description
     */
    private String actionDescription;
}
