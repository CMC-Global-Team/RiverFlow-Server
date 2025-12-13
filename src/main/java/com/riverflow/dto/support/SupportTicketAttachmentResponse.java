package com.riverflow.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for support ticket attachment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketAttachmentResponse {

    private Long id;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private String downloadUrl;
    private LocalDateTime createdAt;
}
