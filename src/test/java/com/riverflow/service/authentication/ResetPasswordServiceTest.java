package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ResetPasswordRequest;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.model.PasswordReset;
import com.riverflow.model.User;
import com.riverflow.repository.PasswordResetRepository;
import com.riverflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResetPasswordService
 */
@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ResetPasswordService resetPasswordService;

    @Test
    void resetPassword_ValidRequest_Success() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newPassword123", "newPassword123");
        User user = User.builder().id(1L).email("test@example.com").build();
        PasswordReset resetToken = PasswordReset.builder()
                .token("valid-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(passwordResetRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPassword");

        // When
        resetPasswordService.resetPassword(request);

        // Then
        verify(userRepository).save(user);
        verify(passwordResetRepository).save(resetToken);
        verify(passwordEncoder).encode("newPassword123");
    }

    @Test
    void resetPassword_PasswordsDoNotMatch_ThrowsException() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newPassword123", "differentPassword");

        // When & Then
        assertThatThrownBy(() -> resetPasswordService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Mật khẩu xác nhận không khớp.");
        
        verify(passwordResetRepository, never()).findByTokenAndUsedAtIsNullAndExpiresAtAfter(anyString(), any());
    }

    @Test
    void resetPassword_InvalidToken_ThrowsException() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "newPassword123", "newPassword123");

        when(passwordResetRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> resetPasswordService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn.");
        
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsException() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "newPassword123", "newPassword123");

        when(passwordResetRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> resetPasswordService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn.");
    }
}
