package com.riverflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for admin user management with all user fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String avatar;
    private String role;
    private Long credit;
    private String status;
    private String preferredLanguage;
    private String timezone;
    private String theme;
    private Boolean emailVerified;
    private String oauthProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
