package com.riverflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity to track user activities for analytics and audit
 */
@Entity
@Table(name = "user_activities", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Activity type (e.g., login, logout, mindmap.create)
     */
    @Column(name = "activity_type", nullable = false, length = 100)
    private String activityType;
    
    /**
     * Entity type (e.g., mindmap, workflow)
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    /**
     * ID of the entity (can be MongoDB ID)
     */
    @Column(name = "entity_id", length = 100)
    private String entityId;
    
    /**
     * IP address
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Additional context about the activity
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private String details;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

