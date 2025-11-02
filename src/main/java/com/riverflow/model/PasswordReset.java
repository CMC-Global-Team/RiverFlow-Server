package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for managing password reset tokens
 */
@Entity
@Table(name = "password_resets", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordReset {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User requesting password reset
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_password_reset_user"))
    @NotNull(message = "User is required")
    private User user;
    
    /**
     * Unique reset token
     */
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Token is required")
    private String token;
    
    /**
     * Token expiration timestamp
     */
    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Expiration time is required")
    private LocalDateTime expiresAt;
    
    /**
     * Timestamp when token was used
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

