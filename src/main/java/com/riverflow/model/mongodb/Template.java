package com.riverflow.model.mongodb;

import com.riverflow.model.mongodb.subdocuments.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mindmap templates for quick start
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
    
    private String thumbnail; // URL to template thumbnail
    
    @Indexed
    private String category; // work, personal, education, etc.
    
    private List<String> tags;
    
    // Template content
    private TemplateData templateData;
    
    // Template metadata
    private Long createdBy; // MySQL user ID who created the template
    
    @Indexed
    private Boolean isOfficial = false; // Official template from system
    
    private Boolean isPublic = true;
    
    // AI Integration
    private Long aiWorkflowId; // Associated AI workflow ID
    
    // Usage statistics
    @Indexed
    private Integer usageCount = 0;
    
    @Indexed
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
        private List<Node> nodes;
        private List<Edge> edges;
        private Map<String, Object> settings;
    }
}

