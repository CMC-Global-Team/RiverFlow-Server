package com.riverflow.service.user;

import com.riverflow.dto.authentication.UpdateUserRequest;
import com.riverflow.dto.authentication.UserResponse;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling user profile operations
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get user information by ID
     */
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate avatar URL if avatar data exists
        // Note: Return /user/avatar/{userId} (without /api prefix) since client baseURL already includes /api
        String avatarUrl = null;
        if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
            avatarUrl = "/user/avatar/" + userId;
        } else if (user.getAvatar() != null) {
            // Fallback to legacy URL-based avatar
            avatarUrl = user.getAvatar();
        }

        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(avatarUrl)
                .role(user.getRole() != null ? user.getRole().name().toUpperCase() : "USER")
                .credit(user.getCredit() != null ? user.getCredit() : 0L)
                .preferredLanguage(user.getPreferredLanguage())
                .timezone(user.getTimezone())
                .theme(user.getTheme() != null ? user.getTheme().name() : "light")
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Email cannot be changed - ignore email from request
        // Update user fields (excluding email)
        user.setFullName(request.getFullName());
        // Email is not updated - it remains unchanged
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        userRepository.save(user);

        // Generate avatar URL if avatar data exists
        // Note: Return /user/avatar/{userId} (without /api prefix) since client baseURL already includes /api
        String avatarUrl = null;
        if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
            avatarUrl = "/user/avatar/" + userId;
        } else if (user.getAvatar() != null) {
            // Fallback to legacy URL-based avatar
            avatarUrl = user.getAvatar();
        }

        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(avatarUrl)
                .role(user.getRole() != null ? user.getRole().name().toUpperCase() : "USER")
                .credit(user.getCredit() != null ? user.getCredit() : 0L)
                .preferredLanguage(user.getPreferredLanguage())
                .timezone(user.getTimezone())
                .theme(user.getTheme() != null ? user.getTheme().name() : "light")
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Get all users (Admin only)
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all users with pagination (Admin only)
     */
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    /**
     * Delete user by ID (Admin only)
     * Soft delete by setting status to 'deleted'
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete
        user.setStatus(User.UserStatus.deleted);
        userRepository.save(user);
    }

    /**
     * Hard delete user by ID (Admin only - use with caution)
     */
    @Transactional
    public void hardDeleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(userId);
    }

    /**
     * Helper method to convert User entity to UserResponse
     */
    private UserResponse convertToResponse(User user) {
        String avatarUrl = null;
        if (user.getAvatarData() != null && user.getAvatarData().length > 0) {
            avatarUrl = "/user/avatar/" + user.getId();
        } else if (user.getAvatar() != null) {
            avatarUrl = user.getAvatar();
        }

        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(avatarUrl)
                .role(user.getRole() != null ? user.getRole().name().toUpperCase() : "USER")
                .credit(user.getCredit() != null ? user.getCredit() : 0L)
                .preferredLanguage(user.getPreferredLanguage())
                .timezone(user.getTimezone())
                .theme(user.getTheme() != null ? user.getTheme().name() : "light")
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}

