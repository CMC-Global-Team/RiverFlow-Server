package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing canvas size
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanvasSize {
    
    private Integer width = 5000;
    private Integer height = 5000;
}

