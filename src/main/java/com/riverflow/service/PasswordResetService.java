package com.riverflow.service;

import com.riverflow.dto.PasswordResetRequestDTO;
import com.riverflow.model.PasswordReset;
import com.riverflow.model.User;
import com.riverflow.repository.PasswordResetRepository;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j

public class PasswordResetService {

    private final PasswordResetRepository passwordResetRepository;
    private final UserRepository userRepository;
    @Transactional
    public void changePassword(PasswordResetRequestDTO request) {
        String token = request.getToken();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        // Find valid token
        PasswordReset reset = passwordResetRepository
                .findByTokenAndUsedAtIsNullAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> {
                    log.warn("Invalid or expired password reset token: {}", token);
                    return new IllegalArgumentException("Invalid or expired reset token");
                });

        // Update the user's password
        User user = reset.getUser();
        user.setPasswordHash(newPassword);
        userRepository.save(user);

        // Mark token as used
        reset.setUsedAt(LocalDateTime.now());
        passwordResetRepository.save(reset);

        log.info("Password successfully changed for user ID {}", user.getId());
    }
}