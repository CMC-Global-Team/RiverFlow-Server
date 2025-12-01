package com.riverflow.service.authentication;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.SignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.exception.EmailNotVerifiedException;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import com.riverflow.util.authentication.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SignInService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class SignInServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SignInService signInService;

    private SignInRequest signInRequest;
    private UserDetails userDetails;
    private User testUser;

    @BeforeEach
    void setUp() {
        signInRequest = new SignInRequest();
        signInRequest.setEmail("test@example.com");
        signInRequest.setPassword("password123");

        userDetails = new org.springframework.security.core.userdetails.User(
                "test@example.com",
                "hashedPassword",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .fullName("Test User")
                .emailVerified(true)
                .role(User.Role.user)
                .credit(100L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void signIn_ValidCredentialsAndVerifiedEmail_ReturnsSignInResponse() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token-123");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token-456");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        SignInResponse response = signInService.signIn(signInRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-123");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-456");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getCredit()).isEqualTo(100L);
        
        // Verify interactions
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateAccessToken(userDetails);
        verify(jwtUtil).generateRefreshToken(userDetails);
        verify(userRepository).save(testUser);
    }

    @Test
    void signIn_UnverifiedEmail_ThrowsEmailNotVerifiedException() {
        // Given
        testUser.setEmailVerified(false);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);

        // When & Then
        assertThatThrownBy(() -> signInService.signIn(signInRequest))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("Email chưa được xác thực");

        // Verify tokens were not generated
        verify(jwtUtil, never()).generateAccessToken(any());
        verify(jwtUtil, never()).generateRefreshToken(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signIn_InvalidCredentials_ThrowsBadCredentialsException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThatThrownBy(() -> signInService.signIn(signInRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Email hoặc mật khẩu không chính xác");

        // Verify no further processing
        verify(userDetailsService, never()).loadUserEntityByEmail(anyString());
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void signIn_SuccessfulLogin_UpdatesLastLoginTime() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        signInService.signIn(signInRequest);

        // Then
        verify(userRepository).save(argThat(user -> 
            user.getLastLoginAt() != null && 
            user.getEmail().equals("test@example.com")
        ));
    }

    @Test
    void signIn_UserWithAvatarData_ReturnsAvatarUrl() {
        // Given
        testUser.setAvatarData(new byte[]{1, 2, 3});
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        SignInResponse response = signInService.signIn(signInRequest);

        // Then
        assertThat(response.getAvatar()).isEqualTo("/user/avatar/1");
    }

    @Test
    void signIn_UserWithLegacyAvatar_ReturnsLegacyUrl() {
        // Given
        testUser.setAvatar("https://example.com/avatar.jpg");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        SignInResponse response = signInService.signIn(signInRequest);

        // Then
        assertThat(response.getAvatar()).isEqualTo("https://example.com/avatar.jpg");
    }
}
