package com.riverflow.dto.authentication;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GitHubSignInRequest {

    @NotBlank
    private String code; // GitHub OAuth authorization code from frontend
}
