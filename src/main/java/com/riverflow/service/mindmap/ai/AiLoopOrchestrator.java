package com.riverflow.service.mindmap.ai;

import com.riverflow.dto.mindmap.MindmapResponse;

import java.util.Map;

public interface AiLoopOrchestrator {
    MindmapResponse run(String mindmapId,
                        Long userId,
                        String prompt,
                        String language,
                        Map<String, Object> workspace,
                        Integer maxIterations,
                        String mode);
}





