package com.riverflow.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB document for tracking mindmap changes
 * Used for undo/redo and audit trail
 */
@Document(collection = "mindmap_history")
@CompoundIndexes({
    @CompoundIndex(name = "idx_mindmap_created", def = "{'mindmapId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_user_created", def = "{'mysqlUserId': 1, 'createdAt': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapHistory {
    
    @Id
    private String id;
    
    /**
     * Reference to the mindmap
     */
    @Indexed
    private String mindmapId;
    
    /**
     * User who made the change
     */
    @Indexed
    private Long mysqlUserId;
    
    /**
     * Action performed
     */
    @Indexed
    private HistoryAction action;
    
    /**
     * Delta/diff of what changed (for efficient storage)
     */
    private Map<String, Object> changes;
    
    /**
     * Full snapshot (stored periodically, not for every change)
     */
    private Map<String, Object> snapshot;
    
    /**
     * Request metadata
     */
    private HistoryMetadata metadata;
    
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;
    
    public enum HistoryAction {
        CREATE,
        UPDATE,
        DELETE,
        NODE_ADD,
        NODE_UPDATE,
        NODE_DELETE,
        NODE_MOVE,
        EDGE_ADD,
        EDGE_UPDATE,
        EDGE_DELETE,
        SETTINGS_UPDATE,
        COLLABORATOR_ADD,
        COLLABORATOR_REMOVE,
        RESTORE
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoryMetadata {
        private String ip;
        private String userAgent;
        private String sessionId;
    }
}

