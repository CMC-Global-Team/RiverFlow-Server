package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for tracking important system actions
 * Used for security auditing and debugging
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User who performed the action (null for system actions)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_audit_log_user"))
    private User user;
    
    /**
     * Action performed (e.g., "user.login", "user.register", "payment.completed")
     */
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Action is required")
    private String action;
    
    /**
     * Type of entity affected (e.g., "user", "payment", "subscription")
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    /**
     * ID of the affected entity
     */
    @Column(name = "entity_id")
    private Long entityId;
    
    // Request info
    
    /**
     * IP address of the request
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    // Details
    
    /**
     * Additional context about the action (stored as JSON)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, Object> details;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

