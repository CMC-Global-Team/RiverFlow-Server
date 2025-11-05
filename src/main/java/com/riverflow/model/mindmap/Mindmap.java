package com.riverflow.model.mindmap;

import com.riverflow.model.mindmap.subdocuments.Collaborator;
import com.riverflow.model.mindmap.subdocuments.MindmapMetadata;
import com.riverflow.model.mindmap.subdocuments.MindmapSettings;
import com.riverflow.model.mindmap.subdocuments.Viewport;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main Mindmap document - ReactFlow compatible
 */
@Document(collection = "mindmaps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mindmap {
    
    @Id
    private String id;
    
    @Indexed
    private Long mysqlUserId;
    
    @TextIndexed
    @Indexed
    private String title;
    
    @TextIndexed
    private String description;
    
    private String thumbnail;
    
    // ReactFlow data - stored as List<Map> for flexibility
    @Builder.Default
    private List<Map<String, Object>> nodes = new ArrayList<>();
    
    @Builder.Default
    private List<Map<String, Object>> edges = new ArrayList<>();
    
    // ReactFlow viewport
    private Viewport viewport;
    
    // Canvas settings
    private MindmapSettings settings;
    
    // Sharing & Collaboration
    @Builder.Default
    @Indexed
    private Boolean isPublic = false;
    
    @Indexed
    private String shareToken;
    
    @Builder.Default
    private List<Collaborator> collaborators = new ArrayList<>();
    
    // Organization
    private List<String> tags;
    
    private String category; // work, personal, education, project, brainstorming, ai-generated, other
    
    @Builder.Default
    private Boolean isFavorite = false;
    
    @Builder.Default
    private Boolean isTemplate = false;
    
    @Indexed
    @Builder.Default
    private String status = "active"; // active, archived, deleted
    
    // AI Integration
    @Builder.Default
    private Boolean aiGenerated = false;
    
    private Long aiWorkflowId;
    
    private Map<String, Object> aiMetadata;
    
    // Metadata
    private MindmapMetadata metadata;
    
    @Indexed
    private LocalDateTime createdAt;
    
    @Indexed
    private LocalDateTime updatedAt;
}

