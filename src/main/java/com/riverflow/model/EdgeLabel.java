package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing edge label
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeLabel {
    
    private String text;
    private Double position = 0.5; // 0-1, position along the edge
}

