package com.riverflow.model.mindmap;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mindmap templates
 */
@Document(collection = "templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {
    
    @Id
    private String id;
    
    private String name;
    
    private String description;
    
    private String thumbnail;
    
    @Indexed
    private String category; // work, personal, education, project, brainstorming, ai-generated, other
    
    private List<String> tags;
    
    // ReactFlow template data
    private TemplateData templateData;
    
    private Long createdBy;
    
    @Indexed
    @Builder.Default
    private Boolean isOfficial = false;
    
    @Builder.Default
    private Boolean isPublic = true;
    
    private Long aiWorkflowId;
    
    @Indexed
    @Builder.Default
    private Integer usageCount = 0;
    
    @Indexed
    @Builder.Default
    private String status = "active"; // active, archived, deleted
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Template data structure
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateData {
        private List<Map<String, Object>> nodes;
        private List<Map<String, Object>> edges;
        private Map<String, Object> viewport;
        private Map<String, Object> settings;
    }
}

