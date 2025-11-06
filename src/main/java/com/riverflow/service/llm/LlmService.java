package com.riverflow.service.llm;

import com.riverflow.dto.mindmap.AiMindmapRequest;

/**
 * Service interface for LLM integration
 * Supports multiple LLM providers (OpenAI, Claude, etc.)
 */
public interface LlmService {
    
    /**
     * Generate mindmap nodes and edges using LLM
     * 
     * @param request AI mindmap request with context
     * @return JSON string with nodes and edges in React Flow format
     */
    String generateMindmapContent(AiMindmapRequest request);
    
    /**
     * Check if LLM service is available
     */
    boolean isAvailable();
}






