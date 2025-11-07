package com.riverflow.controller.authentication;

import com.riverflow.dto.authentication.SignInRequest;
import com.riverflow.dto.authentication.SignInResponse;
import com.riverflow.service.authentication.SignInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user sign-in
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API xác thực người dùng")
public class SignInController {

    private final SignInService signInService;

    /**
     * API Endpoint: Đăng nhập
     * POST /api/auth/signin
     * 
     * Endpoint này không yêu cầu authentication.
     * Sau khi đăng nhập thành công, bạn sẽ nhận được JWT token để sử dụng cho các API khác.
     */
    @PostMapping("/signin")
    @Operation(
        summary = "Đăng nhập",
        description = "Đăng nhập và nhận JWT token. Token này sẽ được sử dụng trong header 'Authorization: Bearer {token}' cho các API khác."
    )
    @SecurityRequirements() // Không yêu cầu authentication cho endpoint này
    public ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest request) {
        SignInResponse response = signInService.signIn(request);
        return ResponseEntity.ok(response);
    }
}

