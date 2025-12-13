package com.riverflow.dto.support;

import com.riverflow.model.support.SupportTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for support ticket details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketResponse {

    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private SupportTicket.Category category;
    private SupportTicket.Priority priority;
    private SupportTicket.Status status;

    // User info
    private Long userId;
    private String userEmail;
    private String userFullName;

    // Assignment info
    private Long assignedToId;
    private String assignedToEmail;
    private String assignedToFullName;

    // Messages
    private List<SupportTicketMessageResponse> messages;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
}
