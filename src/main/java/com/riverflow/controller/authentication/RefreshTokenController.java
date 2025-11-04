package com.riverflow.controller.authentication;

import com.riverflow.dto.authentication.RefreshTokenRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.service.authentication.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for refresh token
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    /**
     * API Endpoint: Làm mới access token
     * POST /api/auth/refresh-token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<SignInResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        SignInResponse response = refreshTokenService.refreshToken(request);
        return ResponseEntity.ok(response);
    }
}

