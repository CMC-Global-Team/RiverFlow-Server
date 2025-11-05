package com.riverflow.dto.mindmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for mindmap canvas settings - ReactFlow compatible
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapSettingsDto {
    
    private Boolean fitView;
    
    private Boolean snapToGrid;
    
    private List<Integer> snapGrid; // [x, y]
    
    private Boolean nodesDraggable;
    
    private Boolean nodesConnectable;
    
    private Boolean elementsSelectable;
    
    private Boolean panOnDrag;
    
    private Boolean panOnScroll;
    
    private Boolean zoomOnScroll;
    
    private Boolean zoomOnPinch;
    
    private Boolean zoomOnDoubleClick;
    
    private Map<String, Object> defaultEdgeOptions;
    
    private String connectionMode; // strict, loose
}

