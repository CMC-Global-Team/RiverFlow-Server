package com.riverflow.controller.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.authentication.ChangePasswordRequest;
import com.riverflow.service.authentication.ChangePassService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Controller tests for ChangePassController using MockMvc
 */
@WebMvcTest(controllers = ChangePassController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import(com.riverflow.config.TestSecurityConfig.class)
class ChangePassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChangePassService changePassService;

    @Test
    @WithMockUser(username = "test@example.com")
    void changePassword_ValidRequest_ReturnsSuccess() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        doNothing().when(changePassService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"));

        verify(changePassService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "nonexistent@example.com")
    void changePassword_UserNotFound_ReturnsNotFound() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"))
                .when(changePassService).changePassword(eq("nonexistent@example.com"), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(changePassService).changePassword(eq("nonexistent@example.com"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "oauth@example.com")
    void changePassword_OAuthUser_ReturnsBadRequest() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản OAuth không thể đổi mật khẩu"))
                .when(changePassService).changePassword(eq("oauth@example.com"), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(changePassService).changePassword(eq("oauth@example.com"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void changePassword_IncorrectCurrentPassword_ReturnsBadRequest() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác"))
                .when(changePassService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(changePassService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void changePassword_PasswordMismatch_ReturnsBadRequest() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword123");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không khớp"))
                .when(changePassService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(changePassService).changePassword(eq("test@example.com"), any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_NoPrincipal_ThrowsException() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        // When & Then - No principal provided, should fail
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());

        // Service should not be called without principal
        verify(changePassService, never()).changePassword(any(), any());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void changePassword_ValidatesEmailFromPrincipal() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        String userEmail = "user@example.com";
        doNothing().when(changePassService).changePassword(eq(userEmail), any(ChangePasswordRequest.class));

        // When
        mockMvc.perform(post("/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Verify correct email from principal was used
        verify(changePassService).changePassword(eq(userEmail), any(ChangePasswordRequest.class));
    }
}
