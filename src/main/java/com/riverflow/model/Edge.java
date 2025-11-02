package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing an edge connecting nodes in a mindmap
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Edge {
    
    private String id; // Unique edge identifier
    
    private String source; // Source node ID
    
    private String target; // Target node ID
    
    private EdgeType type = EdgeType.CURVED;
    
    private EdgeStyle style;
    
    private EdgeLabel label;
    
    private Boolean animated = false;
    
    public enum EdgeType {
        STRAIGHT,
        CURVED,
        BEZIER,
        STEP
    }
}

