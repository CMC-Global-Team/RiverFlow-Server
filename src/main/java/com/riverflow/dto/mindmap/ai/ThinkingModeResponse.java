package com.riverflow.dto.mindmap.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response from Thinking Mode containing optimized specs and action plan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThinkingModeResponse {

    // Natural language explanation sent to user
    private String explanation;

    // Optimized topic extracted/refined from user prompt
    private String optimizedTopic;

    // Optimized title for the mindmap
    private String optimizedTitle;

    // AI-determined structure type
    private String structureType;

    // AI-determined number of levels
    private Integer levels;

    // AI-determined first level count
    private Integer firstLevelCount;

    // AI-extracted/refined tags
    private List<String> tags;

    // Language for content
    private String language;

    // Action list - what the Agent should do
    private List<String> actionList;

    // Additional properties for fine-tuning generation
    private Map<String, Object> additionalProperties;

    // Reasoning for the decisions made
    private String reasoning;
}
