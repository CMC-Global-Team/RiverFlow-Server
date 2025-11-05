package com.riverflow.model.mindmap;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Collaboration invitation tracking
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
    
    private Long invitedByUserId;
    
    @Indexed
    private String invitedEmail;
    
    private Long invitedUserId;
    
    private String role; // editor, viewer
    
    @Indexed
    @Builder.Default
    private String status = "pending"; // pending, accepted, rejected, cancelled, expired
    
    @Indexed
    private String token;
    
    private String message;
    
    @Indexed
    private LocalDateTime expiresAt;
    
    private LocalDateTime acceptedAt;
    
    private Map<String, Object> metadata; // sentViaEmail, emailSentAt, reminderCount
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

