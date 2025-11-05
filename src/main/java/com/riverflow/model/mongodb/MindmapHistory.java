package com.riverflow.model.mongodb;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Track all changes to mindmaps for undo/redo and audit trail
 */
@Document(collection = "mindmap_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapHistory {
    
    @Id
    private String id;
    
    @Indexed
    private String mindmapId;
    
    @Indexed
    private Long mysqlUserId; // User who made the change
    
    @Indexed
    private String action; // create, update, delete, node_add, etc.
    
    private Map<String, Object> changes; // Delta/diff of what changed
    
    private Map<String, Object> snapshot; // Full snapshot (stored periodically)
    
    private Map<String, Object> metadata; // ip, userAgent, sessionId
    
    @Indexed
    private LocalDateTime createdAt;
}

