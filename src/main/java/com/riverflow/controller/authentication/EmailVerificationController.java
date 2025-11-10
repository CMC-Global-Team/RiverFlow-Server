package com.riverflow.controller.authentication;

import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.authentication.ResendVerificationRequest;
import com.riverflow.service.authentication.EmailVerificationServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for email verification
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationController {

    private final EmailVerificationServiceImpl emailVerificationService;

    /**
     * API Endpoint: Xác thực email
     * GET /api/auth/verify-email?token=xxx
     */
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        // Trim token from request parameter
        if (token != null) {
            token = token.trim();
        }
        
        log.info("Email verification request received for token: {} (length: {})", 
            token != null ? token.substring(0, Math.min(8, token.length())) + "..." : "null",
            token != null ? token.length() : 0);
        try {
            emailVerificationService.verifyEmail(token);
            log.info("Email verification successful for token: {}", token != null ? token.substring(0, Math.min(8, token.length())) + "..." : "null");
            return ResponseEntity.ok(new MessageResponse("Xác thực email thành công! Bạn có thể đăng nhập."));
        } catch (Exception e) {
            log.error("Email verification failed for token: {} (length: {}) - Error: {} - Exception type: {}", 
                token != null ? token.substring(0, Math.min(8, token.length())) + "..." : "null",
                token != null ? token.length() : 0,
                e.getMessage(),
                e.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * Alias: /api/auth/verify?token=xxx
     */
    @GetMapping("/verify")
    public ResponseEntity<MessageResponse> verifyEmailAlias(@RequestParam("token") String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(new MessageResponse("Xác thực email thành công! Bạn có thể đăng nhập."));
    }

    /**
     * API Endpoint: Gửi lại email xác minh
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request);
        return ResponseEntity.ok(new MessageResponse("Đã gửi lại email xác minh. Vui lòng kiểm tra hộp thư."));
    }
}

