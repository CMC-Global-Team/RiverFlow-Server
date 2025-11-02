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
 * MongoDB document for mindmap activity feed
 * Used for notifications and activity logs
 */
@Document(collection = "mindmap_activities")
@CompoundIndexes({
    @CompoundIndex(name = "idx_mindmap_created", def = "{'mindmapId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_user_created", def = "{'mysqlUserId': 1, 'createdAt': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapActivity {
    
    @Id
    private String id;
    
    /**
     * Reference to the mindmap
     */
    @Indexed
    private String mindmapId;
    
    /**
     * User who performed the activity
     */
    @Indexed
    private Long mysqlUserId;
    
    /**
     * Type of activity
     */
    @Indexed
    private ActivityType activityType;
    
    /**
     * Human-readable description
     */
    private String description;
    
    /**
     * Additional context about the activity
     */
    private Map<String, Object> metadata;
    
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;
    
    public enum ActivityType {
        CREATED,
        UPDATED,
        VIEWED,
        SHARED,
        FORKED,
        COMMENTED,
        COLLABORATOR_ADDED,
        COLLABORATOR_REMOVED,
        VERSION_CREATED,
        EXPORTED
    }
}

