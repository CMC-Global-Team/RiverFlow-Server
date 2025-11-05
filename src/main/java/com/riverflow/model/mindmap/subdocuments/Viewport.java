package com.riverflow.model.mindmap.subdocuments;

import lombok.*;

/**
 * ReactFlow viewport settings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Viewport {
    @Builder.Default
    private Double x = 0.0;
    
    @Builder.Default
    private Double y = 0.0;
    
    @Builder.Default
    private Double zoom = 1.0; // 0.1 - 4.0
}

