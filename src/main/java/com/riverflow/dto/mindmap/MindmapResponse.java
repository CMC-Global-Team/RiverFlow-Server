package com.riverflow.dto.mindmap;

import com.riverflow.model.mindmap.subdocuments.Collaborator;
import com.riverflow.model.mindmap.subdocuments.MindmapMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for full mindmap details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapResponse {
    
    private String id;
    
    private Long mysqlUserId;
    
    private String title;
    
    private String description;
    
    private String thumbnail;
    
    // ReactFlow data
    private List<Map<String, Object>> nodes;
    
    private List<Map<String, Object>> edges;
    
    // Viewport settings
    private ViewportDto viewport;
    
    // Canvas settings
    private MindmapSettingsDto settings;
    
    // Sharing & Collaboration
    private Boolean isPublic;
    
    private String shareToken;
    
    private List<Collaborator> collaborators;
    
    // Organization
    private List<String> tags;
    
    private String category;
    
    private Boolean isFavorite;
    
    private Boolean isTemplate;
    
    private String status;
    
    // AI Integration
    private Boolean aiGenerated;
    
    private Long aiWorkflowId;
    
    private Map<String, Object> aiMetadata;
    
    // Metadata
    private MindmapMetadata metadata;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    private boolean canUndo;
    private boolean canRedo;
}

