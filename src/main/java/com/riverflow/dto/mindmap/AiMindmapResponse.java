package com.riverflow.dto.mindmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for AI Mindmap Assistant
 * Returns nodes and edges in React Flow format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMindmapResponse {
    
    /**
     * List of nodes in React Flow format
     */
    private List<Map<String, Object>> nodes;
    
    /**
     * List of edges in React Flow format
     */
    private List<Map<String, Object>> edges;
}







