package com.riverflow.controller.authentication;

import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.authentication.ForgotPasswordRequest;
import com.riverflow.service.authentication.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for forgot password
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@org.springframework.web.bind.annotation.CrossOrigin(origins = "*", allowedHeaders = "*")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    /**
     * API Endpoint: Quên mật khẩu
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordService.sendPasswordResetEmail(request);
        return ResponseEntity.ok(new MessageResponse("Nếu email tồn tại, link đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư."));
    }
}

