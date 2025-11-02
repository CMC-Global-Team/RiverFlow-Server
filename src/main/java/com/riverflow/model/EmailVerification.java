package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for managing email verification tokens
 */
@Entity
@Table(name = "email_verifications", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User requesting email verification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_email_verification_user"))
    @NotNull(message = "User is required")
    private User user;
    
    /**
     * Unique verification token
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
     * Timestamp when verification was completed
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

