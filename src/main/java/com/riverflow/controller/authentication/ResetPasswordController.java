package com.riverflow.controller.authentication;

import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.authentication.ResetPasswordRequest;
import com.riverflow.service.authentication.ResetPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for reset password
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    /**
     * API Endpoint: Đặt lại mật khẩu
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới."));
    }
}

