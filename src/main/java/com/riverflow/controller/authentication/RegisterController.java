package com.riverflow.controller.authentication;

import com.riverflow.dto.authentication.RegisterRequest;
import com.riverflow.dto.authentication.RegisterResponse;
import com.riverflow.service.authentication.RegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user registration
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@org.springframework.web.bind.annotation.CrossOrigin(origins = "*", allowedHeaders = "*")
public class RegisterController {

    private final RegisterService registerService;

    /**
     * API Endpoint: Đăng ký người dùng mới
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

