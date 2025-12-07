package com.riverflow.controller.authentication;

import com.riverflow.dto.authentication.Auth0SignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.service.authentication.Auth0AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Auth0 OIDC authentication.
 * Handles sign-in requests with Auth0 ID tokens.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class Auth0AuthController {

    private final Auth0AuthService auth0AuthService;

    /**
     * Sign in with Auth0.
     * Validates the Auth0 ID token and returns internal access/refresh tokens.
     * 
     * @param request The Auth0 sign-in request containing the ID token
     * @return SignInResponse with access token, refresh token, and user info
     */
    @PostMapping("/auth0")
    public ResponseEntity<SignInResponse> signInWithAuth0(@Valid @RequestBody Auth0SignInRequest request) {
        SignInResponse response = auth0AuthService.authenticateWithAuth0(request);
        return ResponseEntity.ok(response);
    }
}
