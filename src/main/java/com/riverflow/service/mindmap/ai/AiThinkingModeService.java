package com.riverflow.service.mindmap.ai;

import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.ai.ActionList;
import com.riverflow.dto.mindmap.ai.Otmz;

import java.util.List;

public interface AiThinkingModeService {

    Otmz think(String topic, String language, String structureType, Integer levels, Integer firstLevelCount, List<String> tags, String mode, String mindmapId);

    ActionList plan(Otmz otmz, String language, String mindmapId);

    MindmapResponse generate(ActionList actionList, String mindmapId, String structureType, Long userId);
}
