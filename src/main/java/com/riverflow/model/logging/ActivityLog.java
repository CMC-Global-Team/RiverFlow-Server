package com.riverflow.model.logging;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document for storing activity logs.
 * Used by super admin to track all system activities.
 */
@Document(collection = "activity_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    private String id;

    /**
     * ID of the user who performed the action
     */
    @Indexed
    private Long actorId;

    /**
     * Email of the actor
     */
    private String actorEmail;

    /**
     * Role of the actor (USER, ADMIN, SUPER_ADMIN)
     */
    @Indexed
    private String actorRole;

    /**
     * Action type (e.g., CREATE, UPDATE, DELETE, PAYMENT_SUCCESS, etc.)
     */
    @Indexed
    private String action;

    /**
     * Category of the log (USER_MANAGEMENT, PAYMENT_MANAGEMENT, PAYMENT)
     */
    @Indexed
    private String category;

    /**
     * ID of the affected entity (user ID, payment ID, etc.)
     */
    private String targetId;

    /**
     * Type of the target entity (USER, PAYMENT)
     */
    private String targetType;

    /**
     * Additional details as JSON string
     */
    private String details;

    /**
     * IP address of the request
     */
    private String ipAddress;

    /**
     * User agent string from the request
     */
    private String userAgent;

    /**
     * Timestamp when the action occurred
     */
    @Indexed
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Log categories
     */
    public enum Category {
        USER_MANAGEMENT,
        PAYMENT_MANAGEMENT,
        PAYMENT,
        AUTHENTICATION,
        SYSTEM
    }

    /**
     * Log action types
     */
    public enum Action {
        // User management actions
        USER_CREATE,
        USER_UPDATE,
        USER_DELETE,
        USER_SOFT_DELETE,
        USER_HARD_DELETE,
        USER_RESTORE,
        CREDIT_UPDATE,
        PASSWORD_CHANGE,

        // Payment actions
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_STATUS_UPDATE,

        // Authentication actions
        LOGIN,
        LOGOUT,

        // System actions
        SYSTEM_CONFIG_UPDATE
    }
}
