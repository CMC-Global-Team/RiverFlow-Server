package com.riverflow.dto.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user sign-in response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // in seconds
    
    // User information
    private Long userId;
    private String email;
    private String fullName;
    private String role;
}

