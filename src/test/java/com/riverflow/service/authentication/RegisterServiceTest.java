package com.riverflow.service.authentication;

import com.riverflow.dto.authentication.RegisterRequest;
import com.riverflow.dto.authentication.RegisterResponse;
import com.riverflow.exception.EmailAlreadyExistsException;
import com.riverflow.model.EmailVerification;
import com.riverflow.model.User;
import com.riverflow.repository.EmailVerificationRepository;
import com.riverflow.repository.UserRepository;
import com.riverflow.service.SmtpEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RegisterService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SmtpEmailService smtpEmailService;

    @InjectMocks
    private RegisterService registerService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        // Set up configuration values
        ReflectionTestUtils.setField(registerService, "backendUrl", "http://localhost:8080/api");
        ReflectionTestUtils.setField(registerService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(registerService, "verificationExpireMinutes", 15);

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFullName("New User");
    }

    @Test
    void register_NewUser_CreatesUserSuccessfully() {
        // Given
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword123");
        
        User savedUser = User.builder()
                .id(1L)
                .email("newuser@example.com")
                .fullName("New User")
                .passwordHash("hashedPassword123")
                .emailVerified(false)
                .role(User.Role.user)
                .credit(3L)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(smtpEmailService).sendVerificationEmail(anyString(), anyString());

        // When
        RegisterResponse response = registerService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("newuser@example.com");
        assertThat(response.getFullName()).isEqualTo("New User");
        assertThat(response.getMessage()).contains("Đăng ký thành công");

        // Verify interactions
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
        verify(emailVerificationRepository).save(any(EmailVerification.class));
        verify(smtpEmailService).sendVerificationEmail(eq("newuser@example.com"), anyString());
    }

    @Test
    void register_ExistingEmail_ThrowsEmailAlreadyExistsException() {
        // Given
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> registerService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email newuser@example.com đã được sử dụng");

        // Verify no user was created
        verify(userRepository, never()).save(any());
        verify(emailVerificationRepository, never()).save(any());
        verify(smtpEmailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void register_NewUser_EncodesPassword() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashedPassword123");
        
        User savedUser = User.builder()
                .id(1L)
                .email("newuser@example.com")
                .passwordHash("hashedPassword123")
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(smtpEmailService).sendVerificationEmail(anyString(), anyString());

        // When
        registerService.register(registerRequest);

        // Then
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(argThat(user -> 
            user.getPasswordHash().equals("hashedPassword123")
        ));
    }

    @Test
    void register_NewUser_CreatesVerificationToken() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        
        User savedUser = User.builder()
                .id(1L)
                .email("newuser@example.com")
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(smtpEmailService).sendVerificationEmail(anyString(), anyString());

        // When
        registerService.register(registerRequest);

        // Then
        verify(emailVerificationRepository).save(argThat(verification ->
            verification.getUser().equals(savedUser) &&
            verification.getToken() != null &&
            verification.getExpiresAt() != null
        ));
    }

    @Test
    void register_NewUser_SendsVerificationEmail() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        
        User savedUser = User.builder()
                .id(1L)
                .email("newuser@example.com")
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(smtpEmailService).sendVerificationEmail(anyString(), anyString());

        // When
        registerService.register(registerRequest);

        // Then
        verify(smtpEmailService).sendVerificationEmail(eq("newuser@example.com"), anyString());
    }

    @Test
    void register_NewUser_SetsDefaultValues() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(emailVerificationRepository.save(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(smtpEmailService).sendVerificationEmail(anyString(), anyString());

        User[] capturedUser = new User[1];
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            capturedUser[0] = invocation.getArgument(0);
            capturedUser[0].setId(1L);
            return capturedUser[0];
        });

        // When
        registerService.register(registerRequest);

        // Then
        assertThat(capturedUser[0].getEmailVerified()).isFalse();
        assertThat(capturedUser[0].getRole()).isEqualTo(User.Role.user);
        assertThat(capturedUser[0].getStatus()).isEqualTo(User.UserStatus.active);
        assertThat(capturedUser[0].getOauthProvider()).isEqualTo(User.OAuthProvider.email);
        assertThat(capturedUser[0].getCredit()).isEqualTo(3L);
    }
}
