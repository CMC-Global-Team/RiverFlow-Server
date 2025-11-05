package com.riverflow.service.authentication;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.SignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.exception.EmailNotVerifiedException;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import com.riverflow.util.authentication.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for handling user sign-in
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignInService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    /**
     * Authenticate user and generate JWT tokens
     */
    @Transactional
    public SignInResponse signIn(SignInRequest request) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Load user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userDetailsService.loadUserEntityByEmail(userDetails.getUsername());

            // Check if email is verified
            if (!user.getEmailVerified()) {
                throw new EmailNotVerifiedException("Email chưa được xác thực. Vui lòng kiểm tra email để xác thực tài khoản.");
            }

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("User {} signed in successfully", user.getEmail());

            // Build response
            return SignInResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 1 hour in seconds
                    .userId(user.getId())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role("USER") // All users have USER role
                    .build();

        } catch (EmailNotVerifiedException e) {
            // Re-throw EmailNotVerifiedException để GlobalExceptionHandler xử lý
            log.error("Sign in failed for user {}: Email not verified", request.getEmail());
            throw e;
        } catch (BadCredentialsException e) {
            log.error("Sign in failed for user {}: {}", request.getEmail(), e.getMessage());
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }
    }
}

