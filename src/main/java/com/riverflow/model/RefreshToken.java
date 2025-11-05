package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing JWT refresh tokens
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_expires", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User who owns this refresh token
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_token_user"))
    @NotNull(message = "User is required")
    private User user;
    
    /**
     * The refresh token string
     */
    @Column(nullable = false, unique = true, length = 500)
    @NotBlank(message = "Token is required")
    private String token;
    
    /**
     * Token expiration timestamp
     */
    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Expiration time is required")
    private LocalDateTime expiresAt;
    
    /**
     * Whether this token has been revoked
     */
    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;
    
    /**
     * Timestamp when token was revoked
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    /**
     * Device information (user agent, IP, etc.)
     */
    @Column(name = "device_info", length = 500)
    private String deviceInfo;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

