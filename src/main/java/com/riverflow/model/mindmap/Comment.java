package com.riverflow.model.mindmap;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comments on mindmap nodes
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
    private String nodeId;
    
    private Long mysqlUserId;
    
    private String content;
    
    private List<Long> mentions;
    
    @Builder.Default
    private Boolean resolved = false;
    
    private Long resolvedBy;
    
    private LocalDateTime resolvedAt;
    
    private String parentCommentId;
    
    @Builder.Default
    private Boolean isEdited = false;
    
    private LocalDateTime editedAt;
    
    @Indexed
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

