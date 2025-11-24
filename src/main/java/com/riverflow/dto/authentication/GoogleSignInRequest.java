package com.riverflow.dto.authentication;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleSignInRequest {

	@NotBlank
	private String credential; // Google ID token from frontend
}


