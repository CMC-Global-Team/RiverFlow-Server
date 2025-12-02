package com.riverflow.controller.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.authentication.ResendVerificationRequest;
import com.riverflow.exception.InvalidTokenException;
import com.riverflow.service.authentication.EmailVerificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for EmailVerificationController using MockMvc
 */
@WebMvcTest(controllers = EmailVerificationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailVerificationServiceImpl emailVerificationService;

    @Test
    void verifyEmail_ValidToken_ReturnsSuccess() throws Exception {
        // Given
        String validToken = "valid-token-123";
        doNothing().when(emailVerificationService).verifyEmail(validToken);

        // When & Then
        mockMvc.perform(get("/auth/verify-email")
                        .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xác thực email thành công! Bạn có thể đăng nhập."));

        verify(emailVerificationService).verifyEmail(validToken);
    }

    @Test
    void verifyEmail_TokenWithWhitespace_TrimsAndVerifies() throws Exception {
        // Given
        String tokenWithWhitespace = "  token-with-spaces  ";
        String trimmedToken = "token-with-spaces";
        doNothing().when(emailVerificationService).verifyEmail(trimmedToken);

        // When & Then
        mockMvc.perform(get("/auth/verify-email")
                        .param("token", tokenWithWhitespace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xác thực email thành công! Bạn có thể đăng nhập."));

        verify(emailVerificationService).verifyEmail(trimmedToken);
    }

    @Test
    void verifyEmail_InvalidToken_ThrowsException() throws Exception {
        // Given
        String invalidToken = "invalid-token";
        doThrow(new InvalidTokenException("Token xác thực không hợp lệ."))
                .when(emailVerificationService).verifyEmail(invalidToken);

        // When & Then
        mockMvc.perform(get("/auth/verify-email")
                        .param("token", invalidToken))
                .andExpect(status().isBadRequest());

        verify(emailVerificationService).verifyEmail(invalidToken);
    }

    @Test
    void verifyEmail_ExpiredToken_ThrowsException() throws Exception {
        // Given
        String expiredToken = "expired-token";
        doThrow(new InvalidTokenException("Token đã hết hạn. Vui lòng yêu cầu link mới."))
                .when(emailVerificationService).verifyEmail(expiredToken);

        // When & Then
        mockMvc.perform(get("/auth/verify-email")
                        .param("token", expiredToken))
                .andExpect(status().isBadRequest());

        verify(emailVerificationService).verifyEmail(expiredToken);
    }

    @Test
    void verifyEmail_AlreadyUsedToken_ThrowsException() throws Exception {
        // Given
        String usedToken = "used-token";
        doThrow(new InvalidTokenException("Token này đã được sử dụng."))
                .when(emailVerificationService).verifyEmail(usedToken);

        // When & Then
        mockMvc.perform(get("/auth/verify-email")
                        .param("token", usedToken))
                .andExpect(status().isBadRequest());

        verify(emailVerificationService).verifyEmail(usedToken);
    }

    @Test
    void verifyEmailAlias_ValidToken_ReturnsSuccess() throws Exception {
        // Given
        String validToken = "valid-token-alias";
        doNothing().when(emailVerificationService).verifyEmail(validToken);

        // When & Then
        mockMvc.perform(get("/auth/verify")
                        .param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xác thực email thành công! Bạn có thể đăng nhập."));

        verify(emailVerificationService).verifyEmail(validToken);
    }

    @Test
    void verifyEmailAlias_InvalidToken_ThrowsException() throws Exception {
        // Given
        String invalidToken = "invalid-alias-token";
        doThrow(new InvalidTokenException("Token xác thực không hợp lệ."))
                .when(emailVerificationService).verifyEmail(invalidToken);

        // When & Then
        mockMvc.perform(get("/auth/verify")
                        .param("token", invalidToken))
                .andExpect(status().isBadRequest());

        verify(emailVerificationService).verifyEmail(invalidToken);
    }

    @Test
    void resendVerification_ValidEmail_ReturnsSuccess() throws Exception {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("user@example.com");
        doNothing().when(emailVerificationService).resendVerification(any(ResendVerificationRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã gửi lại email xác minh. Vui lòng kiểm tra hộp thư."));

        verify(emailVerificationService).resendVerification(any(ResendVerificationRequest.class));
    }

    @Test
    void resendVerification_AlreadyVerified_ThrowsException() throws Exception {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("verified@example.com");
        doThrow(new InvalidTokenException("Email đã được xác minh trước đó."))
                .when(emailVerificationService).resendVerification(any(ResendVerificationRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(emailVerificationService).resendVerification(any(ResendVerificationRequest.class));
    }

    @Test
    void resendVerification_UserNotFound_ThrowsException() throws Exception {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("nonexistent@example.com");
        doThrow(new InvalidTokenException("Không tìm thấy người dùng với email này."))
                .when(emailVerificationService).resendVerification(any(ResendVerificationRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(emailVerificationService).resendVerification(any(ResendVerificationRequest.class));
    }

    @Test
    void resendVerification_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("invalid-email");

        // When & Then
        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(emailVerificationService, never()).resendVerification(any());
    }

    @Test
    void resendVerification_EmptyEmail_ReturnsBadRequest() throws Exception {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest("");

        // When & Then
        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(emailVerificationService, never()).resendVerification(any());
    }
}
