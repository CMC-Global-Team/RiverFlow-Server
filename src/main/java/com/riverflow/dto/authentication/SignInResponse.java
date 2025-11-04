package com.riverflow.dto.authentication;

import com.riverflow.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user sign-in response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default // Giữ giá trị mặc định khi dùng Builder
    private String tokenType = "Bearer";
    private Long expiresIn; // (Access token expiration in seconds)

    // User information
    private Long userId;
    private String email;
    private String fullName;
    private String role;

    /**
     * HÀM TRỢ GIÚP (HELPER METHOD)
     * Dùng để dễ dàng chuyển đổi từ User Entity sang Response DTO.
     */
    public static SignInResponse build(User user, String accessToken, String refreshToken, long accessTokenExpirationMs) {
        return SignInResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpirationMs / 1000) // Chuyển ms sang giây
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name()) // Chuyển Enum sang String
                .build();
    }
}