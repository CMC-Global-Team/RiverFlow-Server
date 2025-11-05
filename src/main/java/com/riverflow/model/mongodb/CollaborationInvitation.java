package com.riverflow.model.mongodb;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Track collaboration invitations
 * Useful for managing pending invitations and notifications
 */
@Document(collection = "collaboration_invitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaborationInvitation {
    
    @Id
    private String id;
    
    @Indexed
    private String mindmapId;
    
    private Long invitedByUserId; // MySQL user ID who sent invitation
    
    @Indexed
    private String invitedEmail;
    
    private Long invitedUserId; // MySQL user ID if user exists
    
    private String role; // editor, viewer
    
    @Indexed
    private String status = "pending"; // pending, accepted, rejected, cancelled, expired
    
    @Indexed
    private String token; // Invitation token for email link
    
    private String message; // Personal message from inviter
    
    @Indexed
    private LocalDateTime expiresAt;
    
    private LocalDateTime acceptedAt;
    
    private Map<String, Object> metadata; // sentViaEmail, emailSentAt, reminderCount
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

