package com.riverflow.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Embedded document representing a collaborator in a mindmap
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collaborator {
    
    private Long mysqlUserId; // MySQL user ID
    
    private CollaboratorRole role;
    
    private Long invitedBy; // MySQL user ID who sent invitation
    
    private LocalDateTime invitedAt = LocalDateTime.now();
    
    private LocalDateTime acceptedAt;
    
    private CollaboratorStatus status = CollaboratorStatus.PENDING;
    
    public enum CollaboratorRole {
        OWNER,
        EDITOR,
        VIEWER
    }
    
    public enum CollaboratorStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        REMOVED
    }
}

