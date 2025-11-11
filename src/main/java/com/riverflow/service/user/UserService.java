package com.riverflow.service.user;

import com.riverflow.dto.authentication.UpdateUserRequest;
import com.riverflow.dto.authentication.UserResponse;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user profile operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get user information by ID
     */
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
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
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getTimezone() != null) {
            user.setTimezone(request.getTimezone());
        }

        userRepository.save(user);

        log.info("User {} profile updated", user.getEmail());

        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
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

