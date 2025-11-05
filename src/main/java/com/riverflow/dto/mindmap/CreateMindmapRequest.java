package com.riverflow.dto.mindmap;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new mindmap
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMindmapRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
    
    private String thumbnail;
    
    // ReactFlow data
    private List<Map<String, Object>> nodes;
    
    private List<Map<String, Object>> edges;
    
    // Viewport settings
    private ViewportDto viewport;
    
    // Canvas settings
    private MindmapSettingsDto settings;
    
    // Organization
    private List<String> tags;
    
    private String category; // work, personal, education, project, brainstorming, ai-generated, other
    
    private Boolean isPublic;
    
    private Boolean isFavorite;
    
    private Boolean isTemplate;
    
    // AI Integration
    private Boolean aiGenerated;
    
    private Long aiWorkflowId;
    
    private Map<String, Object> aiMetadata;
}

