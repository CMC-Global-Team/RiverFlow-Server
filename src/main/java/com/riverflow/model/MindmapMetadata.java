package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing mindmap metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapMetadata {
    
    private Integer nodeCount = 0;
    private Integer edgeCount = 0;
    private Long lastEditedBy; // MySQL user ID
    private Integer viewCount = 0;
    private Integer forkCount = 0;
    private String forkedFrom; // MongoDB ObjectId
}

