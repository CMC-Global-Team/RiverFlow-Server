package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ForgotPasswordRequest;
import com.riverflow.model.PasswordReset;
import com.riverflow.model.User;
import com.riverflow.repository.PasswordResetRepository;
import com.riverflow.repository.UserRepository;
import com.riverflow.service.SmtpEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling forgot password
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final SmtpEmailService smtpEmailService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.verification.expire-minutes:15}")
    private int resetTokenExpireMinutes;

    /**
     * Send password reset email
     */
    @Transactional
    public void sendPasswordResetEmail(ForgotPasswordRequest request) {
        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Password reset requested for non-existent email: {}", request.getEmail());
                    return new IllegalArgumentException("Tài khoản với email này chưa được đăng ký");
                });

        // Generate reset token
        String token = UUID.randomUUID().toString();
        PasswordReset resetToken = PasswordReset.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(resetTokenExpireMinutes))
                .build();

        passwordResetRepository.save(resetToken);

        // Send reset email via SMTP Server
        smtpEmailService.sendResetPasswordEmail(user.getEmail(), token);
        log.info("Password reset email sent to {} via SMTP server", user.getEmail());
    }
}

