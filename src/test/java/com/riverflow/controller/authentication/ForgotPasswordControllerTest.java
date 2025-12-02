package com.riverflow.controller.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.authentication.ForgotPasswordRequest;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.service.authentication.ForgotPasswordService;
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
 * Controller tests for ForgotPasswordController using MockMvc
 */
@WebMvcTest(controllers = ForgotPasswordController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class ForgotPasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ForgotPasswordService forgotPasswordService;

    @Test
    void forgotPassword_ValidEmail_ReturnsSuccess() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@example.com");
        
        doNothing().when(forgotPasswordService).sendPasswordResetEmail(any(ForgotPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Link đặt lại mật khẩu đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư."));

        verify(forgotPasswordService).sendPasswordResetEmail(any(ForgotPasswordRequest.class));
    }

    @Test
    void forgotPassword_UserNotFound_ThrowsException() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent@example.com");
        
        doThrow(new InvalidTokenException("Không tìm thấy người dùng với email này."))
                .when(forgotPasswordService).sendPasswordResetEmail(any(ForgotPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(forgotPasswordService).sendPasswordResetEmail(any(ForgotPasswordRequest.class));
    }

    @Test
    void forgotPassword_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("invalid-email");

        // When & Then
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(forgotPasswordService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void forgotPassword_EmptyEmail_ReturnsBadRequest() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("");

        // When & Then
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(forgotPasswordService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void forgotPassword_NullEmail_ReturnsBadRequest() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(null);

        // When & Then
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(forgotPasswordService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void forgotPassword_OAuthUser_ThrowsException() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("oauth@example.com");
        
        doThrow(new InvalidTokenException("Tài khoản OAuth không thể đặt lại mật khẩu."))
                .when(forgotPasswordService).sendPasswordResetEmail(any(ForgotPasswordRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(forgotPasswordService).sendPasswordResetEmail(any(ForgotPasswordRequest.class));
    }

    @Test
    void forgotPassword_ServiceCalledWithCorrectRequest() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");
        
        doNothing().when(forgotPasswordService).sendPasswordResetEmail(any(ForgotPasswordRequest.class));

        // When
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        verify(forgotPasswordService).sendPasswordResetEmail(argThat(req -> 
            req.getEmail().equals("test@example.com")
        ));
    }
}
