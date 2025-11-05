package com.riverflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for user notifications
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_read", columnList = "is_read"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Notification type (e.g., collaboration_invite, comment_mention)
     */
    @Column(nullable = false, length = 100)
    private String type;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    /**
     * Related entity type (e.g., mindmap, comment)
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    @Column(name = "entity_id", length = 100)
    private String entityId;
    
    /**
     * URL to navigate when clicked
     */
    @Column(name = "action_url", length = 500)
    private String actionUrl;
    
    @Column(name = "action_label", length = 100)
    private String actionLabel;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

