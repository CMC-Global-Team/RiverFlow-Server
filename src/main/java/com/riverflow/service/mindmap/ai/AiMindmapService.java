package com.riverflow.service.mindmap.ai;

import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.ai.GenerateMindmapRequest;
import com.riverflow.dto.mindmap.ai.OptimizeRequest;

public interface AiMindmapService {
    MindmapResponse generateMindmap(GenerateMindmapRequest request, Long userId);
    MindmapResponse optimize(OptimizeRequest request, Long userId);
}



