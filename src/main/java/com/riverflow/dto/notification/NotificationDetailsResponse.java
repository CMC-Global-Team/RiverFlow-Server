package com.riverflow.dto.notification;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Extended notification details for modal display
 * Contains additional data based on notification type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDetailsResponse {

    // Basic notification info
    private Long id;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    // For invitation notifications (PROJECT_INVITE)
    private InvitationDetails invitation;

    // For ticket notifications (TICKET_RESPONSE, TICKET_UPDATE)
    private TicketDetails ticket;

    // For payment notifications (CREDIT_TOPUP_SUCCESS)
    private PaymentDetails payment;

    // For collaborator notifications (PROJECT_LEFT, PROJECT_REMOVED)
    private CollaboratorDetails collaborator;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvitationDetails {
        private String token;
        private String mindmapId;
        private String mindmapTitle;
        private String mindmapDescription;
        private String inviterName;
        private String inviterEmail;
        private String inviterAvatarUrl;
        private String role;
        private String status;
        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketDetails {
        private Long ticketId;
        private String ticketNumber;
        private String title;
        private String status;
        private String category;
        private String priority;
        private String latestMessage;
        private String latestMessageFrom;
        private LocalDateTime latestMessageAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetails {
        private Long transactionId;
        private String transactionNumber;
        private Long amount;
        private Integer creditsAdded;
        private String status;
        private LocalDateTime paidAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollaboratorDetails {
        private String mindmapId;
        private String mindmapTitle;
        private String collaboratorName;
        private String collaboratorEmail;
        private String action; // "left" or "removed"
    }
}
