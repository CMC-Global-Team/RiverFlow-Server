package com.riverflow.controller.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.authentication.RefreshTokenRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.service.authentication.RefreshTokenService;
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
 * Controller tests for RefreshTokenController using MockMvc
 */
@WebMvcTest(controllers = RefreshTokenController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class RefreshTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @Test
    void refreshToken_ValidToken_ReturnsNewTokens() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");
        
        SignInResponse response = new SignInResponse();
        response.setAccessToken("new-access-token");
        response.setRefreshToken("new-refresh-token");
        response.setEmail("user@example.com");
        
        when(refreshTokenService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.email").value("user@example.com"));

        verify(refreshTokenService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void refreshToken_InvalidToken_ThrowsException() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");
        
        when(refreshTokenService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token không hợp lệ hoặc đã hết hạn"));

        // When & Then
        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(refreshTokenService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    void refreshToken_EmptyToken_ReturnsBadRequest() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("");

        // When & Then
        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(refreshTokenService, never()).refreshToken(any());
    }

    @Test
    void refreshToken_NullToken_ReturnsBadRequest() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(null);

        // When & Then
        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(refreshTokenService, never()).refreshToken(any());
    }

    @Test
    void refreshToken_ServiceCalledWithCorrectRequest() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("test-token-123");
        
        SignInResponse response = new SignInResponse();
        
        when(refreshTokenService.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(response);

        // When
        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        verify(refreshTokenService).refreshToken(argThat(req -> 
            req.getRefreshToken().equals("test-token-123")
        ));
    }
}
