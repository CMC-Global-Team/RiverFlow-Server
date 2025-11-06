package com.riverflow.service.mindmap;

import com.riverflow.dto.mindmap.AiMindmapRequest;
import com.riverflow.dto.mindmap.AiMindmapResponse;

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
}






