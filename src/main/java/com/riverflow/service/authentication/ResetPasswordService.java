package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ResetPasswordRequest;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.model.PasswordReset;
import com.riverflow.model.User;
import com.riverflow.repository.PasswordResetRepository;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling password reset
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Reset password using token
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidTokenException("Mật khẩu xác nhận không khớp.");
        }

        // Find valid token
        PasswordReset resetToken = passwordResetRepository
                .findByTokenAndUsedAtIsNullAndExpiresAtAfter(request.getToken(), LocalDateTime.now())
                .orElseThrow(() -> {
                    log.warn("Invalid or expired password reset token");
                    return new InvalidTokenException("Token không hợp lệ hoặc đã hết hạn.");
                });

        // Update user password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }
}

