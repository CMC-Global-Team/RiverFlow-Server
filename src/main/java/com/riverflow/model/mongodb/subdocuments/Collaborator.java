package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Users who can access the mindmap (embedded document)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collaborator {
    private Long mysqlUserId; // MySQL user ID of the collaborator
    private String role; // owner, editor, viewer
    private Long invitedBy; // MySQL user ID who sent the invitation
    private LocalDateTime invitedAt;
    private LocalDateTime acceptedAt;
    private String status = "pending"; // pending, accepted, rejected, removed
}

