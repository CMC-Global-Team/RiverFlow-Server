package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Mindmap display settings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapSettings {
    private String theme = "light"; // light, dark, auto
    private String layout = "mind"; // tree, mind, org, radial, free
    private String direction = "horizontal"; // horizontal, vertical
    private Boolean gridEnabled = true;
    private Boolean snapToGrid = false;
    private Integer gridSize = 20;
    private Double zoom = 1.0; // 0.1 - 3.0
    private CanvasSize canvasSize;
}

