package com.riverflow.controller;

import com.riverflow.dto.ForgotPasswordRequest;
import com.riverflow.dto.VerifyTokenRequest;
import com.riverflow.dto.ResetPasswordRequest;
import com.riverflow.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordResetService resetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req,
                                            @RequestHeader(value = "Origin", required = false) String origin) {
        String frontendUrl = (origin != null) ? origin : "https://your-frontend.example.com";
        resetService.createResetToken(req.getEmail(), frontendUrl);

        // KHÔNG tiết lộ email tồn tại hay không
        return ResponseEntity.ok(Map.of("message", "Nếu email tồn tại, một link đặt lại mật khẩu đã được gửi."));
    }

    @PostMapping("/verify-reset-token")
    public ResponseEntity<?> verifyToken(@RequestBody VerifyTokenRequest req) {
        boolean ok = resetService.validateToken(req.getToken());
        if (ok) return ResponseEntity.ok(Map.of("message", "Token hợp lệ."));
        return ResponseEntity.badRequest().body(Map.of("message", "Token không hợp lệ hoặc đã hết hạn."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        boolean ok = resetService.resetPassword(req.getToken(), req.getNewPassword());
        if (ok) return ResponseEntity.ok(Map.of("message", "Mật khẩu đã được đặt lại."));
        return ResponseEntity.badRequest().body(Map.of("message", "Không thể đặt lại mật khẩu (token sai/het han)."));
    }
}