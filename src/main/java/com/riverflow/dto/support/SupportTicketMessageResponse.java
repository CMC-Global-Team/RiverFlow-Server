package com.riverflow.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for support ticket message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketMessageResponse {

    private Long id;
    private String message;
    private Boolean isInternalNote;

    // Sender info
    private Long senderId;
    private String senderEmail;
    private String senderFullName;
    private String senderRole;
    private String senderAvatarUrl;

    // Attachments
    private List<SupportTicketAttachmentResponse> attachments;

    private LocalDateTime createdAt;
}
