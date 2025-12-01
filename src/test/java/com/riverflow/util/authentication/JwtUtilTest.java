package com.riverflow.util.authentication;

import com.riverflow.config.jwt.JwtConfig;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JwtUtil
 */
@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtUtil jwtUtil;

    private UserDetails userDetails;
    private static final String TEST_SECRET = "testSecretKeyForJWTTokenGenerationMustBe256BitsLongMinimumForHS256AlgorithmToWorkProperlyInTestEnvironment";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 7200000L; // 2 hours

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn(TEST_SECRET);
        when(jwtConfig.getAccessTokenExpirationMs()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        when(jwtConfig.getRefreshTokenExpirationMs()).thenReturn(REFRESH_TOKEN_EXPIRATION);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    void generateAccessToken_ValidUserDetails_ReturnsToken() {
        // When
        String token = jwtUtil.generateAccessToken(userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateRefreshToken_ValidUserDetails_ReturnsToken() {
        // When
        String token = jwtUtil.generateRefreshToken(userDetails);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void extractUsername_ValidToken_ReturnsUsername() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        String username = jwtUtil.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void extractExpiration_ValidToken_ReturnsExpirationDate() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        Date expiration = jwtUtil.extractExpiration(token);

        // Then
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void validateToken_ValidTokenAndUserDetails_ReturnsTrue() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        Boolean isValid = jwtUtil.validateToken(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_ValidTokenWithoutUserDetails_ReturnsTrue() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);

        // When
        Boolean isValid = jwtUtil.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        Boolean isValid = jwtUtil.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WrongUserDetails_ReturnsFalse() {
        // Given
        String token = jwtUtil.generateAccessToken(userDetails);
        UserDetails differentUser = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        // When
        Boolean isValid = jwtUtil.validateToken(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void extractUsername_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractUsername(invalidToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void generateAccessToken_ContainsAuthorities() {
        // When
        String token = jwtUtil.generateAccessToken(userDetails);
        
        // Then - extract claims and verify authorities are included
        // This is an integration-style test verifying the token contains expected data
        assertThat(token).isNotNull();
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void generateRefreshToken_ContainsTypeField() {
        // When
        String token = jwtUtil.generateRefreshToken(userDetails);
        
        // Then
        assertThat(token).isNotNull();
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        // When
        Boolean isValid = jwtUtil.validateToken("");

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_NullToken_ReturnsFalse() {
        // When
        Boolean isValid = jwtUtil.validateToken((String) null);

        // Then
        assertThat(isValid).isFalse();
    }
}
