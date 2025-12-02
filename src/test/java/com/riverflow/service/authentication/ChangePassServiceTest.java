package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.ChangePasswordRequest;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChangePassService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class ChangePassServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChangePassService changePassService;

    private User emailUser;
    private User oauthUser;
    private ChangePasswordRequest validRequest;

    @BeforeEach
    void setUp() {
        // Email-based user
        emailUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedCurrentPassword")
                .oauthProvider(User.OAuthProvider.email)
                .build();

        // OAuth user
        oauthUser = User.builder()
                .id(2L)
                .email("oauth@example.com")
                .oauthProvider(User.OAuthProvider.google)
                .oauthId("google123")
                .build();

        // Valid change password request
        validRequest = new ChangePasswordRequest();
        validRequest.setCurrentPassword("oldPassword123");
        validRequest.setNewPassword("newPassword123");
        validRequest.setConfirmPassword("newPassword123");
    }

    @Test
    void changePassword_ValidRequest_ChangesPasswordSuccessfully() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(emailUser));
        when(passwordEncoder.matches("oldPassword123", "hashedCurrentPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(emailUser);

        // When
        changePassService.changePassword("test@example.com", validRequest);

        // Then
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("oldPassword123", "hashedCurrentPassword");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(argThat(user -> 
            user.getPasswordHash().equals("hashedNewPassword")
        ));
    }

    @Test
    void changePassword_UserNotFound_ThrowsNotFoundException() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> changePassService.changePassword("nonexistent@example.com", validRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Không tìm thấy người dùng")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_OAuthUser_ThrowsBadRequestException() {
        // Given
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.of(oauthUser));

        // When & Then
        assertThatThrownBy(() -> changePassService.changePassword("oauth@example.com", validRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tài khoản OAuth không thể đổi mật khẩu")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository).findByEmail("oauth@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_IncorrectCurrentPassword_ThrowsBadRequestException() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(emailUser));
        when(passwordEncoder.matches("oldPassword123", "hashedCurrentPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> changePassService.changePassword("test@example.com", validRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Mật khẩu hiện tại không chính xác")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("oldPassword123", "hashedCurrentPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_PasswordMismatch_ThrowsBadRequestException() {
        // Given
        ChangePasswordRequest mismatchRequest = new ChangePasswordRequest();
        mismatchRequest.setCurrentPassword("oldPassword123");
        mismatchRequest.setNewPassword("newPassword123");
        mismatchRequest.setConfirmPassword("differentPassword123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(emailUser));
        when(passwordEncoder.matches("oldPassword123", "hashedCurrentPassword")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> changePassService.changePassword("test@example.com", mismatchRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Mật khẩu mới không khớp")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("oldPassword123", "hashedCurrentPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_GoogleOAuthUser_ThrowsBadRequestException() {
        // Given
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.of(oauthUser));

        // When & Then
        assertThatThrownBy(() -> changePassService.changePassword("oauth@example.com", validRequest))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changePassword_GitHubOAuthUser_ThrowsBadRequestException() {
        // Given
        User githubUser = User.builder()
                .id(3L)
                .email("github@example.com")
                .oauthProvider(User.OAuthProvider.github)
                .oauthId("github123")
                .build();

        when(userRepository.findByEmail("github@example.com")).thenReturn(Optional.of(githubUser));

        // When & Then
        assertThatThrownBy(() -> changePassService.changePassword("github@example.com", validRequest))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void changePassword_ValidRequest_EncodesNewPassword() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(emailUser));
        when(passwordEncoder.matches("oldPassword123", "hashedCurrentPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashedNewPassword");

        User[] savedUser = new User[1];
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            savedUser[0] = invocation.getArgument(0);
            return savedUser[0];
        });

        // When
        changePassService.changePassword("test@example.com", validRequest);

        // Then
        assertThat(savedUser[0].getPasswordHash()).isEqualTo("hashedNewPassword");
        verify(passwordEncoder).encode("newPassword123");
    }
}
