package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing mindmap settings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapSettings {
    
    private Theme theme = Theme.LIGHT;
    
    private Layout layout = Layout.MIND;
    
    private Direction direction = Direction.HORIZONTAL;
    
    private Boolean gridEnabled = true;
    
    private Boolean snapToGrid = false;
    
    private Integer gridSize = 20;
    
    private Double zoom = 1.0;
    
    private CanvasSize canvasSize;
    
    public enum Theme {
        LIGHT,
        DARK,
        AUTO
    }
    
    public enum Layout {
        TREE,
        MIND,
        ORG,
        RADIAL,
        FREE
    }
    
    public enum Direction {
        HORIZONTAL,
        VERTICAL
    }
}

