package com.riverflow.service.authentication;

import com.riverflow.config.jwt.CustomUserDetailsService; // Giả định tên
import com.riverflow.dto.authentication.SignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.exception.EmailNotVerifiedException;
import com.riverflow.model.RefreshToken;
import com.riverflow.model.User;
import com.riverflow.repository.RefreshTokenRepository;
import com.riverflow.repository.UserRepository;
import com.riverflow.util.authentication.JwtUtil; // Giả định tên
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired; // <-- THÊM IMPORT
import org.springframework.beans.factory.annotation.Value; // <-- THÊM IMPORT
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit; // <-- 1. THÊM IMPORT NÀY

/**
 * Service for handling user sign-in
 */
@Service
@Slf4j
public class SignInService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    @Autowired
    public SignInService(AuthenticationManager authenticationManager,
                         JwtUtil jwtUtil,
                         CustomUserDetailsService userDetailsService,
                         UserRepository userRepository,
                         RefreshTokenRepository refreshTokenRepository,
                         @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
                         @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Authenticate user and generate JWT tokens
     */
    @Transactional
    public SignInResponse signIn(SignInRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userDetailsService.loadUserEntityByEmail(userDetails.getUsername());

            if (!user.getEmailVerified()) {
                throw new EmailNotVerifiedException("Email chưa được xác thực. Vui lòng kiểm tra email để xác thực tài khoản.");
            }

            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshTokenString = jwtUtil.generateRefreshToken(userDetails);

            // Lưu Refresh Token
            RefreshToken newRefreshToken = new RefreshToken();
            newRefreshToken.setUser(user);
            newRefreshToken.setToken(refreshTokenString);
            newRefreshToken.setExpiresAt(LocalDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS));
            newRefreshToken.setIsRevoked(false);
            refreshTokenRepository.save(newRefreshToken);

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("User {} signed in successfully", user.getEmail());

            // Build response
            return SignInResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshTokenString)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpirationMs / 1000)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .build();

        } catch (EmailNotVerifiedException e) {
            log.error("Sign in failed for user {}: Email not verified", request.getEmail());
            throw e;
        } catch (BadCredentialsException e) {
            log.error("Sign in failed for user {}: {}", request.getEmail(), e.getMessage());
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }
    }
}