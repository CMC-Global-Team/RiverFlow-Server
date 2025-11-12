package com.riverflow.model;

import jakarta.persistence.*;
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
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
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
    private String fullName;
    
    /**
     * URL to user's avatar image (deprecated - use avatar_data instead)
     */
    @Column(length = 500)
    private String avatar;
    
    /**
     * Avatar image binary data (BLOB)
     * Stored directly in database for reliability
     */
    @Lob
    @Column(name = "avatar_data")
    private byte[] avatarData;
    
    /**
     * MIME type of avatar image (e.g., "image/png", "image/jpeg")
     */
    @Column(name = "avatar_mime_type", length = 50)
    private String avatarMimeType;
    
    /**
     * Account status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.active;
    
    // OAuth fields
    
    /**
     * OAuth provider type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private OAuthProvider oauthProvider = OAuthProvider.email;
    
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
     * Preferred language code (en, vi, etc.)
     */
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en";
    
    /**
     * User's timezone
     */
    @Column(length = 50)
    private String timezone = "UTC";
    
    /**
     * UI theme preference
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Theme theme = Theme.light;
    
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
    
    public enum UserStatus {
        active,
        suspended,
        deleted
    }
    
    public enum OAuthProvider {
        email,
        google,
        github,
        facebook
    }
    
    public enum Theme {
        light,
        dark,
        auto
    }
}

