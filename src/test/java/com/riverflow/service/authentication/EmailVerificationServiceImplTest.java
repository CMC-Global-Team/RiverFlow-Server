package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ResendVerificationRequest;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.model.EmailVerification;
import com.riverflow.model.User;
import com.riverflow.repository.EmailVerificationRepository;
import com.riverflow.repository.UserRepository;
import com.riverflow.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailVerificationServiceImpl using Mockito
 */
@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    private User testUser;
    private EmailVerification validToken;
    private EmailVerification expiredToken;
    private EmailVerification usedToken;

    @BeforeEach
    void setUp() {
        // Set configuration values using ReflectionTestUtils
        ReflectionTestUtils.setField(emailVerificationService, "backendUrl", "http://localhost:8080/api");
        ReflectionTestUtils.setField(emailVerificationService, "verificationExpireMinutes", 15);

        // Create test user
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .emailVerified(false)
                .oauthProvider(User.OAuthProvider.email)
                .build();

        // Create valid token
        validToken = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .token("valid-token-123")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .verifiedAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        // Create expired token
        expiredToken = EmailVerification.builder()
                .id(2L)
                .user(testUser)
                .token("expired-token-456")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .verifiedAt(null)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .build();

        // Create already used token
        usedToken = EmailVerification.builder()
                .id(3L)
                .user(testUser)
                .token("used-token-789")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .verifiedAt(LocalDateTime.now().minusMinutes(5))
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
    }

    @Test
    void verifyEmail_ValidToken_VerifiesSuccessfully() {
        // Given
        when(emailVerificationRepository.findByToken("valid-token-123"))
                .thenReturn(Optional.of(validToken));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailVerificationRepository.save(any(EmailVerification.class))).thenReturn(validToken);

        // When
        emailVerificationService.verifyEmail("valid-token-123");

        // Then
        assertThat(testUser.getEmailVerified()).isTrue();
        assertThat(testUser.getEmailVerifiedAt()).isNotNull();
        assertThat(validToken.getVerifiedAt()).isNotNull();
        
        verify(emailVerificationRepository).findByToken("valid-token-123");
        verify(userRepository).save(testUser);
        verify(emailVerificationRepository).save(validToken);
    }

    @Test
    void verifyEmail_NullToken_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(null))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token xác thực không hợp lệ.");

        verify(emailVerificationRepository, never()).findByToken(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_EmptyToken_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail(""))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token xác thực không hợp lệ.");

        verify(emailVerificationRepository, never()).findByToken(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_WhitespaceToken_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("   "))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token xác thực không hợp lệ.");

        verify(emailVerificationRepository, never()).findByToken(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_InvalidToken_ThrowsException() {
        // Given
        when(emailVerificationRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());
        when(emailVerificationRepository.findAll()).thenReturn(Collections.emptyList());

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("invalid-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token xác thực không hợp lệ.");

        verify(emailVerificationRepository).findByToken("invalid-token");
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyEmail_ExpiredToken_ThrowsException() {
        // Given
        when(emailVerificationRepository.findByToken("expired-token-456"))
                .thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("expired-token-456"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token đã hết hạn. Vui lòng yêu cầu link mới.");

        verify(emailVerificationRepository).findByToken("expired-token-456");
        verify(userRepository, never()).save(any());
        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    void verifyEmail_AlreadyUsedToken_ThrowsException() {
        // Given
        when(emailVerificationRepository.findByToken("used-token-789"))
                .thenReturn(Optional.of(usedToken));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmail("used-token-789"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token này đã được sử dụng.");

        verify(emailVerificationRepository).findByToken("used-token-789");
        verify(userRepository, never()).save(any());
        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    void verifyEmail_ValidToken_UpdatesUserAndToken() {
        // Given
        when(emailVerificationRepository.findByToken("valid-token-123"))
                .thenReturn(Optional.of(validToken));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailVerificationRepository.save(any(EmailVerification.class))).thenReturn(validToken);

        LocalDateTime beforeVerification = LocalDateTime.now();

        // When
        emailVerificationService.verifyEmail("valid-token-123");

        // Then
        assertThat(testUser.getEmailVerified()).isTrue();
        assertThat(testUser.getEmailVerifiedAt()).isAfterOrEqualTo(beforeVerification);
        assertThat(validToken.getVerifiedAt()).isAfterOrEqualTo(beforeVerification);
        
        verify(userRepository).save(argThat(user -> 
            user.getEmailVerified().equals(true) && user.getEmailVerifiedAt() != null
        ));
        verify(emailVerificationRepository).save(argThat(token -> 
            token.getVerifiedAt() != null
        ));
    }

    @Test
    void verifyEmail_TokenWithWhitespace_TrimsAndVerifies() {
        // Given
        String tokenWithWhitespace = "  valid-token-123  ";
        when(emailVerificationRepository.findByToken("valid-token-123"))
                .thenReturn(Optional.of(validToken));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailVerificationRepository.save(any(EmailVerification.class))).thenReturn(validToken);

        // When
        emailVerificationService.verifyEmail(tokenWithWhitespace);

        // Then
        verify(emailVerificationRepository).findByToken("valid-token-123");
        assertThat(testUser.getEmailVerified()).isTrue();
    }

    @Test
    void resendVerification_ValidEmail_CreatesNewToken() {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailVerificationRepository).deleteAllByUser(testUser);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        // When
        emailVerificationService.resendVerification(request);

        // Then
        verify(userRepository).findByEmail("test@example.com");
        verify(emailVerificationRepository).deleteAllByUser(testUser);
        
        ArgumentCaptor<EmailVerification> tokenCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(tokenCaptor.capture());
        
        EmailVerification savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getToken()).isNotNull();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
        
        verify(emailService).sendSimpleMessage(
                eq("test@example.com"),
                eq("Gửi lại liên kết xác minh RiverFlow"),
                anyString()
        );
    }

    @Test
    void resendVerification_UserNotFound_ThrowsException() {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.resendVerification(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Không tìm thấy người dùng với email này.");

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(emailVerificationRepository, never()).deleteAllByUser(any());
        verify(emailVerificationRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerification_AlreadyVerified_ThrowsException() {
        // Given
        User verifiedUser = User.builder()
                .id(2L)
                .email("verified@example.com")
                .fullName("Verified User")
                .emailVerified(true)
                .emailVerifiedAt(LocalDateTime.now().minusDays(1))
                .build();
        
        ResendVerificationRequest request = new ResendVerificationRequest("verified@example.com");
        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.resendVerification(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Email đã được xác minh trước đó.");

        verify(userRepository).findByEmail("verified@example.com");
        verify(emailVerificationRepository, never()).deleteAllByUser(any());
        verify(emailVerificationRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void resendVerification_DeletesOldTokens() {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailVerificationRepository).deleteAllByUser(testUser);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        // When
        emailVerificationService.resendVerification(request);

        // Then
        verify(emailVerificationRepository).deleteAllByUser(testUser);
    }

    @Test
    void resendVerification_SendsEmail() {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailVerificationRepository).deleteAllByUser(testUser);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        // When
        emailVerificationService.resendVerification(request);

        // Then
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(emailService).sendSimpleMessage(
                emailCaptor.capture(),
                subjectCaptor.capture(),
                bodyCaptor.capture()
        );

        assertThat(emailCaptor.getValue()).isEqualTo("test@example.com");
        assertThat(subjectCaptor.getValue()).isEqualTo("Gửi lại liên kết xác minh RiverFlow");
        assertThat(bodyCaptor.getValue())
                .contains("Test User")
                .contains("http://localhost:8080/api/auth/verify?token=")
                .contains("15 phút");
    }

    @Test
    void resendVerification_CreatesTokenWithCorrectExpiration() {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doNothing().when(emailVerificationRepository).deleteAllByUser(testUser);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

        LocalDateTime beforeCreation = LocalDateTime.now().plusMinutes(15).minusSeconds(1);

        // When
        emailVerificationService.resendVerification(request);

        // Then
        ArgumentCaptor<EmailVerification> tokenCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(tokenCaptor.capture());
        
        EmailVerification savedToken = tokenCaptor.getValue();
        LocalDateTime afterCreation = LocalDateTime.now().plusMinutes(15).plusSeconds(1);
        
        assertThat(savedToken.getExpiresAt()).isBetween(beforeCreation, afterCreation);
    }
}
