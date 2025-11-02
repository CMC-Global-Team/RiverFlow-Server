package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing node size
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeSize {
    
    private Integer width = 150;
    private Integer height = 50;
}

