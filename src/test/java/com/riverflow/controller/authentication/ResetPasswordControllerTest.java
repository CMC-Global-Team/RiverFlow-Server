package com.riverflow.controller.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.authentication.ResetPasswordRequest;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.service.authentication.ResetPasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ResetPasswordController using MockMvc
 */
@WebMvcTest(controllers = ResetPasswordController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class ResetPasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResetPasswordService resetPasswordService;

    @Test
    void resetPassword_ValidRequest_ReturnsSuccess() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-reset-token");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        
        doNothing().when(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới."));

        verify(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_InvalidToken_ThrowsException() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        
        doThrow(new InvalidTokenException("Token đặt lại mật khẩu không hợp lệ."))
                .when(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_ExpiredToken_ThrowsException() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");
        
        doThrow(new InvalidTokenException("Token đã hết hạn. Vui lòng yêu cầu link mới."))
                .when(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_PasswordMismatch_ThrowsException() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword123");
        
        doThrow(new InvalidTokenException("Mật khẩu mới không khớp."))
                .when(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPassword_EmptyToken_ReturnsBadRequest() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        // When & Then
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resetPasswordService, never()).resetPassword(any());
    }

    @Test
    void resetPassword_EmptyPassword_ReturnsBadRequest() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("");
        request.setConfirmPassword("");

        // When & Then
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resetPasswordService, never()).resetPassword(any());
    }

    @Test
    void resetPassword_NullFields_ReturnsBadRequest() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        // All fields null

        // When & Then
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(resetPasswordService, never()).resetPassword(any());
    }

    @Test
    void resetPassword_ServiceCalledWithCorrectRequest() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("test-token-123");
        request.setNewPassword("testPassword456");
        request.setConfirmPassword("testPassword456");
        
        doNothing().when(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));

        // When
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        verify(resetPasswordService).resetPassword(argThat(req -> 
            req.getToken().equals("test-token-123") &&
            req.getNewPassword().equals("testPassword456") &&
            req.getConfirmPassword().equals("testPassword456")
        ));
    }
}
