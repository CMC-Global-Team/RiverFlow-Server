package com.riverflow.service.mindmap;

import com.riverflow.dto.mindmap.AiMindmapRequest;
import com.riverflow.dto.mindmap.AiMindmapResponse;
import com.riverflow.dto.mindmap.GeneratedMindmapNode;

import java.util.List;
import java.util.Map;

/**
 * Service interface for AI Mindmap Assistant operations
 */
public interface AiMindmapService {
    
    /**
     * Process AI mindmap assistant request
     * Supports: expanding ideas, summarizing, adding nodes, restructuring
     * 
     * @param request AI mindmap request with node context and user instruction
     * @return AI mindmap response with nodes and edges
     */
    AiMindmapResponse processAiRequest(AiMindmapRequest request);
    
    /**
     * Generate mindmap nodes using Spring AI
     * 
     * @param request AI mindmap request with node context and user instruction
     * @return List of generated mindmap nodes
     */
    List<GeneratedMindmapNode> generateMindmapNodes(AiMindmapRequest request);
}






