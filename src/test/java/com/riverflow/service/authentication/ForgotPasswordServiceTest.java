package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ForgotPasswordRequest;
import com.riverflow.model.PasswordReset;
import com.riverflow.model.User;
import com.riverflow.repository.PasswordResetRepository;
import com.riverflow.repository.UserRepository;
import com.riverflow.service.SmtpEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ForgotPasswordService
 */
@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private SmtpEmailService smtpEmailService;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forgotPasswordService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(forgotPasswordService, "resetTokenExpireMinutes", 15);
    }

    @Test
    void sendPasswordResetEmail_ValidEmail_Success() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
        User user = User.builder().id(1L).email("test@example.com").build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // When
        forgotPasswordService.sendPasswordResetEmail(request);

        // Then
        verify(passwordResetRepository).save(any(PasswordReset.class));
        verify(smtpEmailService).sendResetPasswordEmail(eq("test@example.com"), anyString());
    }

    @Test
    void sendPasswordResetEmail_NonExistentEmail_ThrowsException() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> forgotPasswordService.sendPasswordResetEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tài khoản với email này chưa được đăng ký");
        
        verify(passwordResetRepository, never()).save(any());
        verify(smtpEmailService, never()).sendResetPasswordEmail(anyString(), anyString());
    }
}
