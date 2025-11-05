package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Individual node in mindmap (embedded document)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Node {
    private String id; // Unique node identifier (UUID)
    
    private String type = "branch"; // root, branch, leaf, floating
    
    private NodeContent content;
    private NodePosition position;
    private NodeSize size;
    
    private String parent; // Parent node ID
    private List<String> children; // Array of child node IDs
    
    private NodeMetadata metadata;
    
    private Boolean collapsed = false; // Whether children are collapsed
    
    private Long createdBy; // MySQL user ID who created this node
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

