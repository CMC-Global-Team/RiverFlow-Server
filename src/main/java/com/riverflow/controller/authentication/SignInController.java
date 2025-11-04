package com.riverflow.controller.authentication;

import com.riverflow.dto.authentication.SignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.service.authentication.SignInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user sign-in
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SignInController {

    private final SignInService signInService;

    /**
     * API Endpoint: Đăng nhập
     * POST /api/auth/signin
     */
    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest request) {
        SignInResponse response = signInService.signIn(request);
        return ResponseEntity.ok(response);
    }
}

