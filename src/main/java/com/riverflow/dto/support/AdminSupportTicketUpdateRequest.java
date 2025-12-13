package com.riverflow.dto.support;

import com.riverflow.model.support.SupportTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin to update a support ticket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSupportTicketUpdateRequest {

    private SupportTicket.Status status;
    private SupportTicket.Priority priority;
    private Long assignedToId;
}
