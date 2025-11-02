package com.riverflow.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document for tracking collaboration invitations
 * Separate from embedded collaborators for managing pending invitations
 */
@Document(collection = "collaboration_invitations")
@CompoundIndexes({
    @CompoundIndex(name = "idx_mindmap_email", def = "{'mindmapId': 1, 'invitedEmail': 1}"),
    @CompoundIndex(name = "idx_user_status", def = "{'invitedUserId': 1, 'status': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationInvitation {
    
    @Id
    private String id;
    
    /**
     * Reference to the mindmap
     */
    @Indexed
    private String mindmapId;
    
    /**
     * User who sent the invitation
     */
    private Long invitedByUserId;
    
    /**
     * Email address of invitee
     */
    @Indexed
    private String invitedEmail;
    
    /**
     * MySQL user ID if user exists
     */
    private Long invitedUserId;
    
    /**
     * Role being offered
     */
    private InvitationRole role;
    
    /**
     * Invitation status
     */
    @Indexed
    private InvitationStatus status = InvitationStatus.PENDING;
    
    /**
     * Invitation token for email link
     */
    @Indexed(unique = true)
    private String token;
    
    /**
     * Personal message from inviter
     */
    private String message;
    
    /**
     * Expiration timestamp
     */
    @Indexed
    private LocalDateTime expiresAt;
    
    /**
     * Acceptance timestamp
     */
    private LocalDateTime acceptedAt;
    
    /**
     * Email metadata
     */
    private InvitationMetadata metadata;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum InvitationRole {
        EDITOR,
        VIEWER
    }
    
    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        CANCELLED,
        EXPIRED
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvitationMetadata {
        private Boolean sentViaEmail = false;
        private LocalDateTime emailSentAt;
        private Integer reminderCount = 0;
    }
}

