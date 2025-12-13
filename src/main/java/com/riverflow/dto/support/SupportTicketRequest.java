package com.riverflow.dto.support;

import com.riverflow.model.support.SupportTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating a support ticket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private SupportTicket.Category category;

    private SupportTicket.Priority priority;
}
