package com.riverflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing users in the system
 * Supports both email/password and OAuth authentication
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_oauth", columnList = "oauth_provider, oauth_id"),
    @Index(name = "idx_role", columnList = "role"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_preferred_currency", columnList = "preferred_currency_id"),
    @Index(name = "idx_users_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User's email address (unique identifier)
     */
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    /**
     * Hashed password (null for OAuth users)
     */
    @Column(name = "password_hash")
    private String passwordHash;
    
    /**
     * User's full name
     */
    @Column(name = "full_name", nullable = false)
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    /**
     * URL to user's avatar image
     */
    @Column(length = 500)
    private String avatar;
    
    /**
     * User role in the system
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;
    
    /**
     * Account status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    // OAuth fields
    
    /**
     * OAuth provider type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private OAuthProvider oauthProvider = OAuthProvider.EMAIL;
    
    /**
     * ID from OAuth provider
     */
    @Column(name = "oauth_id")
    private String oauthId;
    
    // Email verification
    
    /**
     * Whether email has been verified
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    /**
     * Timestamp when email was verified
     */
    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;
    
    // User preferences
    
    /**
     * User's preferred currency for display
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_currency_id", foreignKey = @ForeignKey(name = "fk_user_preferred_currency"))
    private Currency preferredCurrency;
    
    /**
     * Preferred language code (en, vi, etc.)
     */
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en";
    
    /**
     * User's timezone
     */
    @Column(length = 50)
    private String timezone = "UTC";
    
    // Timestamps
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Last login timestamp
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    // Enums
    
    public enum UserRole {
        ADMIN,
        USER
    }
    
    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        DELETED
    }
    
    public enum OAuthProvider {
        EMAIL,
        GOOGLE,
        GITHUB
    }
}

