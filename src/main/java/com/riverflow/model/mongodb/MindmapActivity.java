package com.riverflow.model.mongodb;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Activity feed for mindmaps (for notifications and activity log)
 */
@Document(collection = "mindmap_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapActivity {
    
    @Id
    private String id;
    
    @Indexed
    private String mindmapId;
    
    @Indexed
    private Long mysqlUserId;
    
    @Indexed
    private String activityType; // created, updated, viewed, shared, etc.
    
    private String description; // Human-readable description
    
    private Map<String, Object> metadata; // Additional context
    
    @Indexed
    private LocalDateTime createdAt;
}

