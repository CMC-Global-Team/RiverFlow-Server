package com.riverflow.service.user;

import com.riverflow.model.User;
import com.riverflow.repository.UserRepository;
import com.riverflow.service.user.AvatarService.AvatarData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AvatarService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private AvatarService avatarService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();
    }

    @Test
    void uploadAvatar_ValidImageFile_UploadsSuccessfully() throws IOException {
        // Given
        byte[] imageData = new byte[]{1, 2, 3, 4, 5};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenReturn(imageData);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        avatarService.uploadAvatar(file, 1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(argThat(user ->
                user.getAvatarData() != null &&
                user.getAvatarData().length == 5 &&
                user.getAvatarMimeType().equals("image/jpeg") &&
                user.getAvatar() == null
        ));
    }

    @Test
    void uploadAvatar_EmptyFile_ThrowsException() {
        // Given
        when(file.isEmpty()).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> avatarService.uploadAvatar(file, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");

        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadAvatar_FileTooLarge_ThrowsException() {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(6 * 1024 * 1024L); // 6MB

        // When & Then
        assertThatThrownBy(() -> avatarService.uploadAvatar(file, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds 5MB limit");

        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadAvatar_InvalidMimeType_ThrowsException() {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");

        // When & Then
        assertThatThrownBy(() -> avatarService.uploadAvatar(file, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only image files (JPEG, PNG, WebP, GIF) are allowed");

        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadAvatar_NullContentType_ThrowsException() {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> avatarService.uploadAvatar(file, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only image files (JPEG, PNG, WebP, GIF) are allowed");

        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadAvatar_UserNotFound_ThrowsException() {
        // Given
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("image/png");
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> avatarService.uploadAvatar(file, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: 999");

        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadAvatar_PngFile_UploadsSuccessfully() throws IOException {
        // Given
        byte[] imageData = new byte[]{1, 2, 3};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(500L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(imageData);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        avatarService.uploadAvatar(file, 1L);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getAvatarMimeType().equals("image/png")
        ));
    }

    @Test
    void uploadAvatar_WebpFile_UploadsSuccessfully() throws IOException {
        // Given
        byte[] imageData = new byte[]{1, 2, 3};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(500L);
        when(file.getContentType()).thenReturn("image/webp");
        when(file.getBytes()).thenReturn(imageData);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        avatarService.uploadAvatar(file, 1L);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getAvatarMimeType().equals("image/webp")
        ));
    }

    @Test
    void uploadAvatar_GifFile_UploadsSuccessfully() throws IOException {
        // Given
        byte[] imageData = new byte[]{1, 2, 3};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(500L);
        when(file.getContentType()).thenReturn("image/gif");
        when(file.getBytes()).thenReturn(imageData);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        avatarService.uploadAvatar(file, 1L);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getAvatarMimeType().equals("image/gif")
        ));
    }

    @Test
    void uploadAvatar_ClearsOldUrlAvatar() throws IOException {
        // Given
        testUser.setAvatar("https://example.com/old-avatar.jpg");
        byte[] imageData = new byte[]{1, 2, 3};
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(500L);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenReturn(imageData);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        avatarService.uploadAvatar(file, 1L);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getAvatar() == null
        ));
    }

    @Test
    void getAvatar_UserHasAvatar_ReturnsAvatarData() {
        // Given
        byte[] avatarData = new byte[]{1, 2, 3, 4, 5};
        testUser.setAvatarData(avatarData);
        testUser.setAvatarMimeType("image/jpeg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<AvatarData> result = avatarService.getAvatar(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getData()).isEqualTo(avatarData);
        assertThat(result.get().getMimeType()).isEqualTo("image/jpeg");
    }

    @Test
    void getAvatar_UserNotFound_ReturnsEmpty() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<AvatarData> result = avatarService.getAvatar(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getAvatar_UserHasNoAvatar_ReturnsEmpty() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<AvatarData> result = avatarService.getAvatar(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getAvatar_AvatarDataIsEmpty_ReturnsEmpty() {
        // Given
        testUser.setAvatarData(new byte[0]);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<AvatarData> result = avatarService.getAvatar(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void deleteAvatar_ValidUser_DeletesAvatar() {
        // Given
        testUser.setAvatarData(new byte[]{1, 2, 3});
        testUser.setAvatarMimeType("image/jpeg");
        testUser.setAvatar("https://example.com/avatar.jpg");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        avatarService.deleteAvatar(1L);

        // Then
        verify(userRepository).save(argThat(user ->
                user.getAvatarData() == null &&
                user.getAvatarMimeType() == null &&
                user.getAvatar() == null
        ));
    }

    @Test
    void deleteAvatar_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> avatarService.deleteAvatar(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found: 999");

        verify(userRepository, never()).save(any());
    }
}
