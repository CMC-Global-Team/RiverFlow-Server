package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing edge styling
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeStyle {
    
    private String strokeColor = "#999999";
    private Integer strokeWidth = 2;
    private StrokeStyle strokeStyle = StrokeStyle.SOLID;
    
    public enum StrokeStyle {
        SOLID,
        DASHED,
        DOTTED
    }
}

