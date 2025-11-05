package com.riverflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity for SaaS platform integration configurations
 */
@Entity
@Table(name = "saas_integrations", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_platform", columnList = "platform_name"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaasIntegration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * Platform name (e.g., Slack, Teams, Notion, etc.)
     */
    @Column(name = "platform_name", nullable = false, length = 100)
    private String platformName;
    
    /**
     * Platform-specific configuration
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private String config;
    
    /**
     * Encrypted access token
     */
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;
    
    /**
     * Encrypted refresh token
     */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;
    
    /**
     * Integration status
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IntegrationStatus status = IntegrationStatus.active;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Enum
    
    public enum IntegrationStatus {
        active,
        inactive,
        error
    }
}

