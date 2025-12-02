package com.riverflow.service.authentication;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.RefreshTokenRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.model.User;
import com.riverflow.util.authentication.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .avatar("http://example.com/avatar.png")
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("password")
                .roles("USER")
                .build();
    }

    @Test
    void refreshToken_ValidToken_ReturnsNewAccessToken() {
        // Given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("new-access-token");

        // When
        SignInResponse response = refreshTokenService.refreshToken(request);

        // Then
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");
        assertThat(response.getAvatar()).isEqualTo("http://example.com/avatar.png");

        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil).extractUsername(refreshToken);
        verify(userDetailsService).loadUserByUsername("test@example.com");
        verify(userDetailsService).loadUserEntityByEmail("test@example.com");
        verify(jwtUtil).generateAccessToken(userDetails);
    }

    @Test
    void refreshToken_InvalidToken_ThrowsException() {
        // Given
        String refreshToken = "invalid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);

        when(jwtUtil.validateToken(refreshToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.refreshToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token không hợp lệ hoặc đã hết hạn");

        verify(jwtUtil).validateToken(refreshToken);
        verify(jwtUtil, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void refreshToken_UserWithAvatarData_ReturnsAvatarUrl() {
        // Given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        testUser.setAvatarData(new byte[]{1, 2, 3}); // Set avatar data

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("new-access-token");

        // When
        SignInResponse response = refreshTokenService.refreshToken(request);

        // Then
        assertThat(response.getAvatar()).isEqualTo("/user/avatar/1");
    }

    @Test
    void refreshToken_UserWithoutAvatar_ReturnsNullAvatar() {
        // Given
        String refreshToken = "valid-refresh-token";
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        
        testUser.setAvatar(null);
        testUser.setAvatarData(null);

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("new-access-token");

        // When
        SignInResponse response = refreshTokenService.refreshToken(request);

        // Then
        assertThat(response.getAvatar()).isNull();
    }
}
