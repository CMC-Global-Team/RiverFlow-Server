package com.riverflow.controller.authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API xác thực người dùng")
public class LogoutController {

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Đăng xuất người dùng (JWT stateless - chỉ cần xóa token ở client)")
    public ResponseEntity<Map<String, Object>> logout(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        // Lấy thông tin user hiện tại (nếu có)
        String username = authentication != null ? authentication.getName() : "unknown";
        
        response.put("success", true);
        response.put("message", "Đăng xuất thành công");
        response.put("user", username);
        
        // Lưu ý: Với JWT stateless, token vẫn còn hiệu lực cho đến khi hết hạn
        // Client cần xóa token ở local storage/cookies
        // Nếu muốn invalidate token ngay lập tức, cần implement token blacklist
        
        return ResponseEntity.ok(response);
    }
}

