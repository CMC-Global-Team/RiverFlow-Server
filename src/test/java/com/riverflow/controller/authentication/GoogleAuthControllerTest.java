package com.riverflow.controller.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.authentication.GoogleSignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.service.authentication.GoogleAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for GoogleAuthController using MockMvc
 */
@WebMvcTest(controllers = GoogleAuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoogleAuthService googleAuthService;

    @Test
    void signInWithGoogle_ValidToken_ReturnsSuccess() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setIdToken("valid-google-token");
        
        SignInResponse response = new SignInResponse();
        response.setAccessToken("jwt-access-token");
        response.setRefreshToken("jwt-refresh-token");
        response.setEmail("user@example.com");
        response.setFullName("Test User");
        
        when(googleAuthService.authenticateWithGoogle(any(GoogleSignInRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("jwt-refresh-token"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(googleAuthService).authenticateWithGoogle(any(GoogleSignInRequest.class));
    }

    @Test
    void signInWithGoogle_InvalidToken_ThrowsException() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setIdToken("invalid-google-token");
        
        when(googleAuthService.authenticateWithGoogle(any(GoogleSignInRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google token không hợp lệ"));

        // When & Then
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(googleAuthService).authenticateWithGoogle(any(GoogleSignInRequest.class));
    }

    @Test
    void signInWithGoogle_EmptyToken_ReturnsBadRequest() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setIdToken("");

        // When & Then
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(googleAuthService, never()).authenticateWithGoogle(any());
    }

    @Test
    void signInWithGoogle_NullToken_ReturnsBadRequest() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setIdToken(null);

        // When & Then
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(googleAuthService, never()).authenticateWithGoogle(any());
    }

    @Test
    void signInWithGoogle_NewUser_CreatesAccountAndReturnsTokens() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setIdToken("new-user-google-token");
        
        SignInResponse response = new SignInResponse();
        response.setAccessToken("new-jwt-access-token");
        response.setRefreshToken("new-jwt-refresh-token");
        response.setEmail("newuser@example.com");
        response.setFullName("New User");
        
        when(googleAuthService.authenticateWithGoogle(any(GoogleSignInRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-jwt-access-token"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));

        verify(googleAuthService).authenticateWithGoogle(any(GoogleSignInRequest.class));
    }

    @Test
    void signInWithGoogle_ExistingUser_ReturnsTokens() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setIdToken("existing-user-token");
        
        SignInResponse response = new SignInResponse();
        response.setAccessToken("existing-jwt-access");
        response.setRefreshToken("existing-jwt-refresh");
        response.setEmail("existing@example.com");
        response.setFullName("Existing User");
        
        when(googleAuthService.authenticateWithGoogle(any(GoogleSignInRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        verify(googleAuthService).authenticateWithGoogle(any(GoogleSignInRequest.class));
    }

    @Test
    void signInWithGoogle_ServiceCalledWithCorrectRequest() throws Exception {
        // Given
        GoogleSignInRequest request = new GoogleSignInRequest();
        request.setIdToken("test-token-abc123");
        
        SignInResponse response = new SignInResponse();
        response.setAccessToken("access");
        response.setRefreshToken("refresh");
        
        when(googleAuthService.authenticateWithGoogle(any(GoogleSignInRequest.class)))
                .thenReturn(response);

        // When
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        verify(googleAuthService).authenticateWithGoogle(argThat(req -> 
            req.getIdToken().equals("test-token-abc123")
        ));
    }
}
