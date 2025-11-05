package com.riverflow.model.mindmap.subdocuments;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Mindmap canvas settings - ReactFlow compatible
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapSettings {
    @Builder.Default
    private Boolean fitView = true;
    
    @Builder.Default
    private Boolean snapToGrid = false;
    
    @Builder.Default
    private List<Integer> snapGrid = List.of(15, 15); // [x, y]
    
    @Builder.Default
    private Boolean nodesDraggable = true;
    
    @Builder.Default
    private Boolean nodesConnectable = true;
    
    @Builder.Default
    private Boolean elementsSelectable = true;
    
    @Builder.Default
    private Boolean panOnDrag = true;
    
    @Builder.Default
    private Boolean panOnScroll = false;
    
    @Builder.Default
    private Boolean zoomOnScroll = true;
    
    @Builder.Default
    private Boolean zoomOnPinch = true;
    
    @Builder.Default
    private Boolean zoomOnDoubleClick = true;
    
    private Map<String, Object> defaultEdgeOptions;
    
    @Builder.Default
    private String connectionMode = "strict"; // strict, loose
}

