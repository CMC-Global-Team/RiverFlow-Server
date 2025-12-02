package com.riverflow.service.user;

import com.riverflow.dto.authentication.UpdateUserRequest;
import com.riverflow.dto.authentication.UserResponse;
import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .passwordHash("hashedPassword")
                .oauthProvider(User.OAuthProvider.email)
                .emailVerified(true)
                .role(User.Role.user)
                .credit(10L)
                .preferredLanguage("en")
                .timezone("UTC")
                .theme(User.Theme.light)
                .status(User.UserStatus.active)
                .build();
        
        testUser.setCreatedAt(now);
        testUser.setUpdatedAt(now);
        testUser.setLastLoginAt(now);
    }

    @Test
    void getUserById_ValidUserId_ReturnsUserResponse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");
        assertThat(response.getRole()).isEqualTo("USER");
        assertThat(response.getCredit()).isEqualTo(10L);
        assertThat(response.getPreferredLanguage()).isEqualTo("en");
        assertThat(response.getTimezone()).isEqualTo("UTC");
        assertThat(response.getTheme()).isEqualTo("light");
        assertThat(response.getEmailVerified()).isTrue();
        
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(999L);
    }

    @Test
    void getUserById_WithAvatarData_ReturnsAvatarUrl() {
        // Given
        byte[] avatarData = new byte[]{1, 2, 3, 4, 5};
        testUser.setAvatarData(avatarData);
        testUser.setAvatarMimeType("image/png");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response.getAvatar()).isEqualTo("/user/avatar/1");
    }

    @Test
    void getUserById_WithLegacyAvatarUrl_ReturnsLegacyUrl() {
        // Given
        testUser.setAvatar("https://example.com/avatar.jpg");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response.getAvatar()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    void getUserById_NoAvatar_ReturnsNullAvatarUrl() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response.getAvatar()).isNull();
    }

    @Test
    void getUserById_AdminRole_ReturnsUppercaseRole() {
        // Given
        testUser.setRole(User.Role.admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(1L);

        // Then
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void updateUser_ValidRequest_UpdatesAndReturnsUser() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .preferredLanguage("vi")
                .timezone("Asia/Ho_Chi_Minh")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateUser(1L, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Updated Name");
        assertThat(response.getPreferredLanguage()).isEqualTo("vi");
        assertThat(response.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(response.getEmail()).isEqualTo("test@example.com"); // Email not changed
        
        verify(userRepository).findById(1L);
        verify(userRepository).save(argThat(user -> 
            user.getFullName().equals("Updated Name") &&
            user.getPreferredLanguage().equals("vi") &&
            user.getTimezone().equals("Asia/Ho_Chi_Minh") &&
            user.getEmail().equals("test@example.com")
        ));
    }

    @Test
    void updateUser_UserNotFound_ThrowsException() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_NullPreferredLanguage_DoesNotUpdateLanguage() {
        // Given
        String originalLanguage = testUser.getPreferredLanguage();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .preferredLanguage(null)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUser(1L, request);

        // Then
        verify(userRepository).save(argThat(user -> 
            user.getPreferredLanguage().equals(originalLanguage)
        ));
    }

    @Test
    void updateUser_NullTimezone_DoesNotUpdateTimezone() {
        // Given
        String originalTimezone = testUser.getTimezone();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .timezone(null)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.updateUser(1L, request);

        // Then
        verify(userRepository).save(argThat(user -> 
            user.getTimezone().equals(originalTimezone)
        ));
    }

    @Test
    void updateUser_EmailInRequest_DoesNotUpdateEmail() {
        // Given
        String originalEmail = testUser.getEmail();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .email("newemail@example.com") // Email in request should be ignored
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateUser(1L, request);

        // Then
        assertThat(response.getEmail()).isEqualTo(originalEmail);
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals(originalEmail)
        ));
    }

    @Test
    void updateUser_WithAvatarData_ReturnsCorrectAvatarUrl() {
        // Given
        byte[] avatarData = new byte[]{1, 2, 3};
        testUser.setAvatarData(avatarData);
        
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateUser(1L, request);

        // Then
        assertThat(response.getAvatar()).isEqualTo("/user/avatar/1");
    }

    @Test
    void updateUser_NullCredit_ReturnsZeroCredit() {
        // Given
        testUser.setCredit(null);
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateUser(1L, request);

        // Then
        assertThat(response.getCredit()).isEqualTo(0L);
    }

    @Test
    void updateUser_NullTheme_ReturnsLightTheme() {
        // Given
        testUser.setTheme(null);
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Updated Name")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateUser(1L, request);

        // Then
        assertThat(response.getTheme()).isEqualTo("light");
    }
}
