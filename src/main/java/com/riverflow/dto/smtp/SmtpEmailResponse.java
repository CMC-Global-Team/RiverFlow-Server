package com.riverflow.dto.smtp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for SMTP email response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmtpEmailResponse {
    private Boolean success;
    private String message;
    private String messageId;
}

