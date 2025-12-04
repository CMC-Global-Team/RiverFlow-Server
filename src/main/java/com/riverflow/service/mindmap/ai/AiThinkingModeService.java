package com.riverflow.service.mindmap.ai;

import com.riverflow.dto.mindmap.ai.ThinkingModeRequest;
import com.riverflow.dto.mindmap.ai.ThinkingModeResponse;

/**
 * Service for Thinking Mode - AI-powered prompt optimization
 * Analyzes user input and provides optimized parameters for mindmap generation
 */
public interface AiThinkingModeService {

    /**
     * Analyze user prompt and return optimized specification
     * 
     * @param request User's raw prompt and preferences
     * @param userId User ID for credit deduction and personalization
     * @return Optimized specification and action list
     */
    ThinkingModeResponse analyzeAndOptimize(ThinkingModeRequest request, Long userId);

    /**
     * Analyze with streaming support for real-time feedback
     * 
     * @param request User's raw prompt and preferences
     * @param userId User ID
     * @param mindmapId Optional mindmap ID for streaming events
     * @return Optimized specification and action list
     */
    ThinkingModeResponse analyzeAndOptimizeWithStreaming(ThinkingModeRequest request, Long userId, String mindmapId);
}
