package com.riverflow.controller.authentication;

import com.riverflow.dto.authentication.GoogleSignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.service.authentication.GoogleAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

	private final GoogleAuthService googleAuthService;

	@PostMapping("/google")
	public ResponseEntity<SignInResponse> signInWithGoogle(@Valid @RequestBody GoogleSignInRequest request) {
		SignInResponse response = googleAuthService.authenticateWithGoogle(request);
		return ResponseEntity.ok(response);
	}
}


