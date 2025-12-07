package com.riverflow.dto.authentication;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Auth0 sign-in.
 * Contains the ID token received from Auth0 after successful authentication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auth0SignInRequest {

    /**
     * The ID token received from Auth0 after successful authentication.
     * This token contains the user's identity claims.
     */
    @NotBlank(message = "ID token is required")
    private String idToken;
}
