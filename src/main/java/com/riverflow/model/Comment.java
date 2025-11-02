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
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document for comments on mindmap nodes
 * Used for collaboration and discussion
 */
@Document(collection = "comments")
@CompoundIndexes({
    @CompoundIndex(name = "idx_mindmap_node_created", def = "{'mindmapId': 1, 'nodeId': 1, 'createdAt': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    
    @Id
    private String id;
    
    /**
     * Reference to the mindmap
     */
    @Indexed
    private String mindmapId;
    
    /**
     * Node this comment is attached to
     */
    @Indexed
    private String nodeId;
    
    /**
     * Comment author (MySQL user ID)
     */
    @Indexed
    private Long mysqlUserId;
    
    /**
     * Comment content
     */
    private String content;
    
    /**
     * MySQL user IDs mentioned in comment
     */
    @Builder.Default
    private List<Long> mentions = new ArrayList<>();
    
    /**
     * Whether comment has been resolved
     */
    @Indexed
    private Boolean resolved = false;
    
    /**
     * User who resolved the comment
     */
    private Long resolvedBy;
    
    /**
     * Resolution timestamp
     */
    private LocalDateTime resolvedAt;
    
    /**
     * Parent comment ID for threaded replies
     */
    private String parentCommentId;
    
    /**
     * Whether comment has been edited
     */
    private Boolean isEdited = false;
    
    /**
     * Last edit timestamp
     */
    private LocalDateTime editedAt;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}

