package com.riverflow.service.authentication;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.RefreshTokenRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.model.User;
import com.riverflow.util.authentication.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for handling refresh token
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Refresh access token using refresh token
     */
    public SignInResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        // Extract username from refresh token
        String username = jwtUtil.extractUsername(refreshToken);

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        User user = userDetailsService.loadUserEntityByEmail(username);

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        log.info("Access token refreshed for user: {}", username);

        // Build response
        return SignInResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Keep the same refresh token
                .tokenType("Bearer")
                .expiresIn(3600L) // 1 hour in seconds
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role("USER") // All users have USER role
                .build();
    }
}

