package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Connection between nodes (embedded document)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Edge {
    private String id; // Unique edge identifier
    private String source; // Source node ID
    private String target; // Target node ID
    private String type = "curved"; // straight, curved, bezier, step
    private EdgeStyle style;
    private EdgeLabel label;
    private Boolean animated = false;
}

