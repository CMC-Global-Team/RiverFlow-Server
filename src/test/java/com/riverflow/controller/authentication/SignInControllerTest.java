package com.riverflow.controller.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.config.TestSecurityConfig;
import com.riverflow.dto.authentication.SignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.exception.EmailNotVerifiedException;
import com.riverflow.service.authentication.SignInService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import org.junit.jupiter.api.Test;

/**
 * Controller tests for SignInController using MockMvc
 */
@WebMvcTest(controllers = SignInController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import(com.riverflow.config.TestSecurityConfig.class)
class SignInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignInService signInService;

    @Test
    void signIn_ValidCredentials_ReturnsOkWithTokens() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("test@example.com", "password123");
        
        SignInResponse response = SignInResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role("USER")
                .credit(100L)
                .build();

        when(signInService.signIn(any(SignInRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.credit").value(100));
    }

    @Test
    void signIn_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("test@example.com", "wrongpassword");
        
        when(signInService.signIn(any(SignInRequest.class)))
                .thenThrow(new BadCredentialsException("Email hoặc mật khẩu không chính xác"));

        // When & Then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signIn_UnverifiedEmail_ReturnsForbidden() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("unverified@example.com", "password123");
        
        when(signInService.signIn(any(SignInRequest.class)))
                .thenThrow(new EmailNotVerifiedException("Email chưa được xác thực"));

        // When & Then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void signIn_EmptyEmail_ReturnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("", "password123");

        // When & Then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signIn_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("notanemail", "password123");

        // When & Then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signIn_EmptyPassword_ReturnsBadRequest() throws Exception {
        // Given
        SignInRequest request = new SignInRequest("test@example.com", "");

        // When & Then
        mockMvc.perform(post("/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
