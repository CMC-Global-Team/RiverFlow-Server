package com.riverflow.controller.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.config.TestSecurityConfig;
import com.riverflow.dto.authentication.RegisterRequest;
import com.riverflow.dto.authentication.RegisterResponse;
import com.riverflow.exception.EmailAlreadyExistsException;
import com.riverflow.service.authentication.RegisterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
 * Controller tests for RegisterController using MockMvc
 */
@WebMvcTest(controllers = RegisterController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@Import(TestSecurityConfig.class)
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterService registerService;

    @Test
    void register_ValidRequest_ReturnsCreatedWithResponse() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password123");
        
        RegisterResponse response = RegisterResponse.builder()
                .message("Đăng ký thành công. Vui lòng kiểm tra email để xác thực.")
                .email("test@example.com")
                .userId(1L)
                .build();

        when(registerService.register(any(RegisterRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Đăng ký thành công. Vui lòng kiểm tra email để xác thực."))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void register_EmailAlreadyExists_ReturnsConflict() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("Test User", "existing@example.com", "password123");
        
        when(registerService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email đã được sử dụng"));

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_EmptyEmail_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("", "password123", "Test User");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("notanemail", "password123", "Test User");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShortPassword_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "123", "Test User");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_EmptyFullName_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_NullFields_ReturnsBadRequest() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
