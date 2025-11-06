package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.RegisterRequest;
import com.riverflow.dto.authentication.RegisterResponse;
import com.riverflow.exception.EmailAlreadyExistsException;
import com.riverflow.model.EmailVerification;
import com.riverflow.model.User;
import com.riverflow.repository.EmailVerificationRepository;
import com.riverflow.repository.UserRepository;
import com.riverflow.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling user registration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.backend.url:http://localhost:8080/api}")
    private String backendUrl;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.verification.expire-minutes:15}")
    private int verificationExpireMinutes;

    /**
     * Register a new user
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with existing email: {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email " + request.getEmail() + " đã được sử dụng.");
        }

        // Create new user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(User.UserStatus.active)
                .oauthProvider(User.OAuthProvider.email)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        // Generate verification token
        String token = UUID.randomUUID().toString();
        EmailVerification verificationToken = new EmailVerification(
                savedUser,
                token,
                LocalDateTime.now().plusMinutes(verificationExpireMinutes)
        );
        emailVerificationRepository.save(verificationToken);

        // Send verification email
        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        String emailBody = "Chào " + savedUser.getFullName() + ",\n\n"
                + "Cảm ơn bạn đã đăng ký. Vui lòng nhấn vào đường link bên dưới để kích hoạt tài khoản của bạn:\n"
                + verificationLink + "\n\n"
                + "Liên kết sẽ hết hạn sau " + verificationExpireMinutes + " phút.\n\n"
                + "Trân trọng,\nĐội ngũ RiverFlow.";

        emailService.sendSimpleMessage(savedUser.getEmail(), "Xác thực tài khoản RiverFlow", emailBody);
        log.info("Verification email sent to {}", savedUser.getEmail());

        // Build response
        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .message("Đăng ký thành công! Vui lòng kiểm tra email để xác thực.")
                .build();
    }
}

