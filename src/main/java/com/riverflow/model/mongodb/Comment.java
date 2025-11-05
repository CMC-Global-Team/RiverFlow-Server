package com.riverflow.model.mongodb;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comments on mindmap nodes (for collaboration)
 */
@Document(collection = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    
    @Id
    private String id;
    
    @Indexed
    private String mindmapId;
    
    @Indexed
    private String nodeId; // Node this comment is attached to
    
    private Long mysqlUserId; // Comment author
    
    private String content;
    
    private List<Long> mentions; // MySQL user IDs mentioned in comment
    
    private Boolean resolved = false;
    
    private Long resolvedBy; // MySQL user ID who resolved
    
    private LocalDateTime resolvedAt;
    
    private String parentCommentId; // For threaded replies
    
    private Boolean isEdited = false;
    
    private LocalDateTime editedAt;
    
    @Indexed
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

