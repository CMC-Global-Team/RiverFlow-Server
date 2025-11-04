package com.riverflow.service;

import com.riverflow.model.PasswordReset;
import com.riverflow.model.User;
import com.riverflow.repository.PasswordResetRepository;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetRepository resetRepository;
    private final MailService mailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Điều chỉnh thời hạn theo nhu cầu (phút)
    private static final long EXPIRATION_MINUTES = 30;

    @Transactional
    public void createResetToken(String email, String frontendBaseUrl) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        // Không tiết lộ email tồn tại hay không -> trả success chung (security)
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();

        // Optional: invalidate các token chưa dùng trước đó hoặc keep nhiều token tùy bạn
        List<PasswordReset> oldTokens = resetRepository.findByUserIdAndUsedAtIsNull(user.getId());
        for (PasswordReset t : oldTokens) {
            t.setUsedAt(LocalDateTime.now());
            resetRepository.save(t);
        }

        String token = UUID.randomUUID().toString();



        PasswordReset pr = PasswordReset.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .build();

        resetRepository.save(pr);
        System.out.println("Generated reset token: " + token);

        String resetLink = frontendBaseUrl + "/auth/reset-password?token=" + token;
        String subject = "Đặt lại mật khẩu RiverFlow";
        String body = "Bạn hoặc ai đó đã yêu cầu đặt lại mật khẩu cho tài khoản này.\n\n"
                + "Nếu bạn muốn đặt lại mật khẩu hãy bấm vào link sau:\n" + resetLink + "\n\n"
                + "Link sẽ hết hạn sau " + EXPIRATION_MINUTES + " phút.\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này.";

        mailService.sendSimpleEmail(user.getEmail(), subject, body);
    }

    public boolean validateToken(String token) {
        Optional<PasswordReset> opt = resetRepository.findByToken(token);
        if (opt.isEmpty()) return false;
        PasswordReset pr = opt.get();
        if (pr.getUsedAt() != null) return false;
        if (pr.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        return true;
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordReset> opt = resetRepository.findByToken(token);
        if (opt.isEmpty()) return false;
        PasswordReset pr = opt.get();

        if (pr.getUsedAt() != null) return false;
        if (pr.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        User user = pr.getUser();
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);

        // mark token as used
        pr.setUsedAt(LocalDateTime.now());
        resetRepository.save(pr);

        // TODO: invalidate user's sessions / JWTs nếu hệ thống bạn có session management
        return true;
    }

    @Transactional
    public void purgeExpiredTokens() {
        resetRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
