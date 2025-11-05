package com.riverflow.dto.mindmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for mindmap list/summary (lightweight)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapSummaryResponse {
    
    private String id;
    
    private Long mysqlUserId;
    
    private String title;
    
    private String description;
    
    private String thumbnail;
    
    private Integer nodeCount;
    
    private Integer edgeCount;
    
    // Organization
    private List<String> tags;
    
    private String category;
    
    private Boolean isFavorite;
    
    private Boolean isTemplate;
    
    private Boolean isPublic;
    
    private String status;
    
    // AI Integration
    private Boolean aiGenerated;
    
    private Long aiWorkflowId;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

