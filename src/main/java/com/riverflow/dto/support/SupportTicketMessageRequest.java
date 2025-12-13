package com.riverflow.dto.support;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a message to a support ticket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketMessageRequest {

    @NotBlank(message = "Message is required")
    private String message;

    /**
     * If true, message is only visible to admin/support staff (admin-only field)
     */
    private Boolean isInternalNote;
}
