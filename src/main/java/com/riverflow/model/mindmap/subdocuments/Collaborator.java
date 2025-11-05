package com.riverflow.model.mindmap.subdocuments;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Collaborator embedded document
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collaborator {
    private Long mysqlUserId;
    private String role; // owner, editor, viewer
    private Long invitedBy;
    private LocalDateTime invitedAt;
    private LocalDateTime acceptedAt;
    
    @Builder.Default
    private String status = "pending"; // pending, accepted, rejected, removed
}

