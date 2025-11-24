package com.riverflow.model.mindmap;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Mindmap history for undo/redo and audit trail
 */
@Document(collection = "mindmap_history")
@CompoundIndexes({
        @CompoundIndex(name = "idx_mindmap_action_createdAt", def = "{'mindmapId': 1, 'action': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "idx_mindmap_status_createdAt", def = "{'mindmapId': 1, 'status': 1, 'createdAt': -1}")
})
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
    private Long mysqlUserId;
    
    @Indexed
    private String action; // create, update, delete, node_add, node_update, node_delete, node_move, edge_add, edge_update, edge_delete, viewport_change, settings_update, collaborator_add, collaborator_remove, restore, ai_generate
    
    private Map<String, Object> changes; // Delta/diff
    
    private Map<String, Object> snapshot; // Periodic full snapshot
    
    private Map<String, Object> metadata; // ip, userAgent, sessionId
    
    @Indexed(expireAfterSeconds = 7776000)
    private LocalDateTime createdAt;

    @Indexed
    @Builder.Default
    private String status = "active";
}

