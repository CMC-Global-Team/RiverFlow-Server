package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing node position
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodePosition {
    
    private Double x = 0.0;
    private Double y = 0.0;
    private Integer z = 0; // Layer order
}

