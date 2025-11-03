package com.riverflow.controller;

import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.auth.RegisterRequest;
import com.riverflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // Đặt URL gốc cho tất cả API trong file này
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * API Endpoint: Đăng ký người dùng mới
     * Kích hoạt validation (@Valid) cho DTO đầu vào.
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

        // Gọi service để xử lý toàn bộ logic
        authService.registerUser(registerRequest);

        // Trả về thông báo thành công (HTTP 200 OK)
        return ResponseEntity.ok(new MessageResponse("Đăng ký thành công! Vui lòng kiểm tra email để xác thực."));
    }

    /**
     * API Endpoint: Xác thực email
     * Lấy token từ query param (ví dụ: ?token=abc-123)
     */
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {

        // Gọi service để xử lý logic xác thực
        authService.verifyEmail(token);

        // Trả về thông báo thành công (HTTP 200 OK)
        return ResponseEntity.ok(new MessageResponse("Xác thực email thành công! Bạn có thể đăng nhập."));
    }
}