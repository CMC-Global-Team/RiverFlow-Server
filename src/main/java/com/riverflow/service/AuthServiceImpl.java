package com.riverflow.service;

import com.riverflow.dto.auth.RegisterRequest;
import com.riverflow.exception.EmailAlreadyExistsException;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.model.EmailVerification;
import com.riverflow.model.User;
import com.riverflow.repository.EmailVerificationRepository;
import com.riverflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    // Tiêm (Inject) các dependency cần thiết
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Đọc URL frontend từ application.properties
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           EmailVerificationRepository emailVerificationRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Xử lý đăng ký
     */
    @Override
    @Transactional
    public User registerUser(RegisterRequest registerRequest) {

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email " + registerRequest.getEmail() + " đã được sử dụng.");
        }

        User user = User.builder()
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                //Mã hóa mật khẩu
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .role(User.UserRole.user)
                .status(User.UserStatus.active)
                .oauthProvider(User.OAuthProvider.email)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        EmailVerification verificationToken = new EmailVerification(
                savedUser,
                token,
                LocalDateTime.now().plusHours(24) // Hết hạn sau 24h
        );

        emailVerificationRepository.save(verificationToken);

        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        String emailBody = "Chào " + savedUser.getFullName() + ",\n\n"
                + "Cảm ơn bạn đã đăng ký. Vui lòng nhấn vào đường link bên dưới để kích hoạt tài khoản của bạn:\n"
                + verificationLink + "\n\n"
                + "Trân trọng,\nĐội ngũ RiverFlow.";

        emailService.sendSimpleMessage(savedUser.getEmail(), "Xác thực tài khoản RiverFlow", emailBody);

        return savedUser;
    }

    /**
     * Xử lý xác thực email
     */
    @Override
    @Transactional
    public void verifyEmail(String token) {
        //Tìm token
        EmailVerification verificationToken = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token xác thực không hợp lệ."));

        //Kiểm tra token đã được sử dụng chưa
        if (verificationToken.getVerifiedAt() != null) {
            throw new InvalidTokenException("Token này đã được sử dụng.");
        }

        //Kiểm tra token đã hết hạn chưa
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token đã hết hạn. Vui lòng yêu cầu link mới.");
        }

        //Lấy user và kích hoạt
        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        //Đánh dấu token đã sử dụng
        verificationToken.setVerifiedAt(LocalDateTime.now());
        emailVerificationRepository.save(verificationToken);
    }
}