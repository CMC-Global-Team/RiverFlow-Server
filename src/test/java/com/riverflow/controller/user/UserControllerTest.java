package com.riverflow.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.authentication.UpdateUserRequest;
import com.riverflow.dto.authentication.UserResponse;
import com.riverflow.model.User;
import com.riverflow.service.user.AvatarService;
import com.riverflow.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for UserController using MockMvc
 */
@WebMvcTest(controllers = UserController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import(com.riverflow.config.TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private AvatarService avatarService;

    private User testUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();

        userResponse = new UserResponse();
        userResponse.setUserId(1L);
        userResponse.setEmail("test@example.com");
        userResponse.setFullName("Test User");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getUserProfile_AuthenticatedUser_ReturnsProfile() throws Exception {
        // Given
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(userService.getUserById(1L)).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(userDetailsService).loadUserEntityByEmail("test@example.com");
        verify(userService).getUserById(1L);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updateUserProfile_ValidRequest_ReturnsUpdatedProfile() throws Exception {
        // Given
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setUserId(1L);
        updatedResponse.setEmail("test@example.com");
        updatedResponse.setFullName("Updated Name");

        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(userService.updateUser(eq(1L), any(UpdateUserRequest.class))).thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));

        verify(userService).updateUser(eq(1L), any(UpdateUserRequest.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void uploadAvatar_ValidFile_ReturnsSuccess() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "test-image-content".getBytes()
        );

        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        doNothing().when(avatarService).uploadAvatar(any(), eq(1L));

        // When & Then
        mockMvc.perform(multipart("/user/avatar/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/user/avatar/1"))
                .andExpect(jsonPath("$.message").value("Avatar uploaded successfully"));

        verify(avatarService).uploadAvatar(any(), eq(1L));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void uploadAvatar_InvalidFile_ReturnsBadRequest() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "test-image-content".getBytes()
        );

        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        doThrow(new IllegalArgumentException("Invalid file format"))
                .when(avatarService).uploadAvatar(any(), eq(1L));

        // When & Then
        mockMvc.perform(multipart("/user/avatar/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid file format"));
    }

    @Test
    void getAvatar_ExistingUser_ReturnsImage() throws Exception {
        // Given
        AvatarService.AvatarData avatarData = new AvatarService.AvatarData(
                "test-image-content".getBytes(),
                "image/png"
        );

        when(avatarService.getAvatar(1L)).thenReturn(Optional.of(avatarData));

        // When & Then
        mockMvc.perform(get("/user/avatar/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes("test-image-content".getBytes()));

        verify(avatarService).getAvatar(1L);
    }

    @Test
    void getAvatar_UserNotFound_ReturnsNotFound() throws Exception {
        // Given
        when(avatarService.getAvatar(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/user/avatar/999"))
                .andExpect(status().isNotFound());

        verify(avatarService).getAvatar(999L);
    }
}
