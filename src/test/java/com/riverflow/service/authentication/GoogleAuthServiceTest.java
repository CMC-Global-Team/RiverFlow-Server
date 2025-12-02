package com.riverflow.service.authentication;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.riverflow.config.GoogleAuthConfig;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.GoogleSignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import com.riverflow.util.authentication.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GoogleAuthService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private GoogleAuthConfig googleAuthConfig;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Spy
    @InjectMocks
    private GoogleAuthService googleAuthService;

    private User testUser;
    private UserDetails userDetails;
    private GoogleIdToken googleIdToken;
    private GoogleIdToken.Payload payload;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .avatar("http://example.com/avatar.png")
                .oauthProvider(User.OAuthProvider.google)
                .oauthId("google-sub-123")
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("password")
                .roles("USER")
                .build();
                
        // Mock GoogleIdToken and Payload
        googleIdToken = mock(GoogleIdToken.class);
        payload = mock(GoogleIdToken.Payload.class);
        lenient().when(googleIdToken.getPayload()).thenReturn(payload);
    }

    @Test
    void authenticateWithGoogle_ValidToken_ReturnsSignInResponse() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setCredential("valid-google-token");
        
        // Mock payload data
        when(payload.getEmail()).thenReturn("test@example.com");
        when(payload.get("name")).thenReturn("Test User");
        when(payload.get("picture")).thenReturn("http://example.com/avatar.png");
        when(payload.getSubject()).thenReturn("google-sub-123");

        // Mock dependencies
        doReturn(googleIdToken).when(googleAuthService).verifyIdToken("valid-google-token");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        // When
        SignInResponse response = googleAuthService.authenticateWithGoogle(request);

        // Then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");
        
        verify(googleAuthService).verifyIdToken("valid-google-token");
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void authenticateWithGoogle_ExistingUser_UpdatesInfo() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setCredential("valid-google-token");
        
        // Mock payload data with updated info
        when(payload.getEmail()).thenReturn("test@example.com");
        when(payload.get("name")).thenReturn("Updated Name");
        when(payload.get("picture")).thenReturn("http://example.com/new-avatar.png");
        when(payload.getSubject()).thenReturn("google-sub-123");

        // Mock dependencies
        doReturn(googleIdToken).when(googleAuthService).verifyIdToken("valid-google-token");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        // When
        googleAuthService.authenticateWithGoogle(request);

        // Then
        verify(userRepository, atLeastOnce()).save(argThat(user -> 
            user.getFullName().equals("Updated Name") &&
            user.getAvatar().equals("http://example.com/new-avatar.png")
        ));
    }

    @Test
    void authenticateWithGoogle_InvalidToken_ThrowsException() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setCredential("invalid-token");
        
        doReturn(null).when(googleAuthService).verifyIdToken("invalid-token");

        // When & Then
        assertThatThrownBy(() -> googleAuthService.authenticateWithGoogle(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Google authentication failed");
    }

    @Test
    void authenticateWithGoogle_VerificationException_ThrowsException() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setCredential("error-token");
        
        doThrow(new RuntimeException("Verification error")).when(googleAuthService).verifyIdToken("error-token");

        // When & Then
        assertThatThrownBy(() -> googleAuthService.authenticateWithGoogle(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Google authentication failed");
    }
}
