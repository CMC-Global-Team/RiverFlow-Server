package com.riverflow.model.mindmap;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Activity feed for mindmaps
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
    private String activityType; // created, updated, viewed, shared, forked, commented, collaborator_added, collaborator_removed, version_created, exported, ai_generated, template_used
    
    private String description;
    
    private Map<String, Object> metadata;
    
    @Indexed
    private LocalDateTime createdAt;
}

