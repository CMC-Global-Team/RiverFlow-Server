package com.riverflow.dto;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ForgotPasswordRequest {
    private String email;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}