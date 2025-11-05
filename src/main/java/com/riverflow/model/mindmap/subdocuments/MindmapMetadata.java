package com.riverflow.model.mindmap.subdocuments;

import lombok.*;

/**
 * Mindmap metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapMetadata {
    @Builder.Default
    private Integer nodeCount = 0;
    
    @Builder.Default
    private Integer edgeCount = 0;
    
    private Long lastEditedBy;
    
    @Builder.Default
    private Integer viewCount = 0;
    
    @Builder.Default
    private Integer forkCount = 0;
    
    private String forkedFrom; // MongoDB ObjectId
}

