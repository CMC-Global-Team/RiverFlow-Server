package com.riverflow.service;

import com.riverflow.model.EmailVerification;
import com.riverflow.model.User;
import com.riverflow.repository.EmailVerificationRepository;
import com.riverflow.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    // private final JavaMailSender mailSender; // nếu muốn gửi email thật

    /**
     * Tạo token xác thực và gửi email
     */
    @Transactional
    public EmailVerification createAndSendVerificationToken(Long userId, int expireMinutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Xóa các token cũ (nếu có)
        emailVerificationRepository.deleteAllByUser(user);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expireMinutes);

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        emailVerificationRepository.save(verification);

        // TODO: Gửi email thật — có thể dùng JavaMailSender hoặc MailService
        System.out.printf("[DEBUG] Send verification link: https://your-domain.com/verify-email?token=%s%n", token);

        return verification;
    }

    /**
     * Xác thực email từ token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<EmailVerification> optionalVerification = emailVerificationRepository.findByToken(token);

        if (optionalVerification.isEmpty()) {
            throw new IllegalArgumentException("Invalid token");
        }

        EmailVerification verification = optionalVerification.get();

        if (verification.getVerifiedAt() != null) {
            throw new IllegalStateException("Token already used");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expired");
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        verification.setVerifiedAt(LocalDateTime.now());
        emailVerificationRepository.save(verification);

        return true;
    }
}
