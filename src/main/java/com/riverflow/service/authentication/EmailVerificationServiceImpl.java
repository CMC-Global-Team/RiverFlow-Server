package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ResendVerificationRequest;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.model.EmailVerification;
import com.riverflow.model.User;
import com.riverflow.repository.EmailVerificationRepository;
import com.riverflow.repository.UserRepository;
import com.riverflow.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling email verification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;

    @Value("${app.backend.url:http://localhost:8080/api}")
    private String backendUrl;

    @Value("${app.verification.expire-minutes:15}")
    private int verificationExpireMinutes;

    /**
     * Verify email using token
     */
    @Transactional
    public void verifyEmail(String token) {
        log.debug("Starting email verification for token: {}", token != null ? token.substring(0, Math.min(8, token.length())) + "..." : "null");
        
        if (token == null || token.trim().isEmpty()) {
            log.warn("Email verification attempted with null or empty token");
            throw new InvalidTokenException("Token xác thực không hợp lệ.");
        }

        // Find token
        EmailVerification verificationToken = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Email verification token not found: {}", token.substring(0, Math.min(8, token.length())) + "...");
                    return new InvalidTokenException("Token xác thực không hợp lệ.");
                });

        log.debug("Found verification token for user: {}", verificationToken.getUser().getEmail());

        // Check if token already used
        if (verificationToken.getVerifiedAt() != null) {
            log.warn("Email verification attempted with already used token for user: {}", verificationToken.getUser().getEmail());
            throw new InvalidTokenException("Token này đã được sử dụng.");
        }

        // Check if token expired
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Email verification attempted with expired token for user: {}, expired at: {}", 
                verificationToken.getUser().getEmail(), verificationToken.getExpiresAt());
            throw new InvalidTokenException("Token đã hết hạn. Vui lòng yêu cầu link mới.");
        }

        // Activate user
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        log.debug("User email verified flag set to true for: {}", user.getEmail());

        // Mark token as used
        verificationToken.setVerifiedAt(LocalDateTime.now());
        emailVerificationRepository.save(verificationToken);
        log.debug("Verification token marked as used");

        log.info("Email verified successfully for user: {}", user.getEmail());
    }

    /**
     * Resend verification email
     */
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidTokenException("Không tìm thấy người dùng với email này."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new InvalidTokenException("Email đã được xác minh trước đó.");
        }

        // Delete all old tokens
        emailVerificationRepository.deleteAllByUser(user);

        // Create new token
        String token = UUID.randomUUID().toString();
        EmailVerification verificationToken = new EmailVerification(
                user,
                token,
                LocalDateTime.now().plusMinutes(verificationExpireMinutes)
        );
        emailVerificationRepository.save(verificationToken);

        // Send verification email
        String verificationLink = backendUrl + "/auth/verify?token=" + token;
        String emailBody = "Chào " + user.getFullName() + ",\n\n"
                + "Bạn vừa yêu cầu gửi lại liên kết xác minh. Vui lòng nhấn vào đường link bên dưới để xác minh email:\n"
                + verificationLink + "\n\n"
                + "Liên kết sẽ hết hạn sau " + verificationExpireMinutes + " phút.\n\n"
                + "Trân trọng,\nĐội ngũ RiverFlow.";

        emailService.sendSimpleMessage(user.getEmail(), "Gửi lại liên kết xác minh RiverFlow", emailBody);
        log.info("Verification email resent to {}", user.getEmail());
    }
}

