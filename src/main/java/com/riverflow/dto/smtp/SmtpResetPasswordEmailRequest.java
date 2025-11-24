package com.riverflow.dto.smtp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for SMTP reset password email request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmtpResetPasswordEmailRequest {
    private String to;
    private String token;
    private String frontendUrl;
}

