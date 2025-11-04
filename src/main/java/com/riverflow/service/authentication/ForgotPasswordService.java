package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ForgotPasswordRequest;
import com.riverflow.model.PasswordReset;
import com.riverflow.model.User;
import com.riverflow.repository.PasswordResetRepository;
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
 * Service for handling forgot password
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final EmailService emailService;

    @Value("${app.backend.url:http://localhost:8080/api}")
    private String backendUrl;

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

        // Send reset email
        String resetLink = backendUrl + "/auth/reset-password?token=" + token;
        String emailBody = "Chào " + user.getFullName() + ",\n\n"
                + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n"
                + "Vui lòng nhấn vào đường link bên dưới để đặt lại mật khẩu:\n"
                + resetLink + "\n\n"
                + "Liên kết sẽ hết hạn sau " + resetTokenExpireMinutes + " phút.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\nĐội ngũ RiverFlow.";

        emailService.sendSimpleMessage(user.getEmail(), "Đặt lại mật khẩu RiverFlow", emailBody);
        log.info("Password reset email sent to {}", user.getEmail());
    }
}

