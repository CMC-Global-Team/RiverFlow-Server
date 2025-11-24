package com.riverflow.dto.smtp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for SMTP email request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmtpEmailRequest {
    private String to;
    private String subject;
    private String html;
    private String text;
}

