package com.riverflow.dto.smtp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmtpInvitationEmailRequest {
    private String to;
    private String token;
    private String inviterName;
    private String mindmapTitle;
    private String frontendUrl;
}