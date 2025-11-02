package com.riverflow.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Embedded document representing a node in a mindmap
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Node {
    
    private String id; // UUID
    
    private NodeType type = NodeType.BRANCH;
    
    private NodeContent content;
    
    private NodePosition position;
    
    private NodeSize size;
    
    private String parent; // Parent node ID
    
    @Builder.Default
    private List<String> children = new ArrayList<>(); // Child node IDs
    
    private NodeMetadata metadata;
    
    private Boolean collapsed = false;
    
    private Long createdBy; // MySQL user ID
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum NodeType {
        ROOT,
        BRANCH,
        LEAF,
        FLOATING
    }
}

