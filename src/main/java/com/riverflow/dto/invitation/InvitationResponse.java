package com.riverflow.dto.invitation;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO for invitation details to display in modal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {

    private String id;
    private String token;

    // Mindmap info
    private String mindmapId;
    private String mindmapTitle;
    private String mindmapDescription;

    // Inviter info
    private Long invitedByUserId;
    private String inviterName;
    private String inviterEmail;
    private String inviterAvatarUrl;

    // Invitation details
    private String invitedEmail;
    private String role;
    private String status;
    private String message;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
