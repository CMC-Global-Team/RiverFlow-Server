package com.riverflow.dto.mindmap.ai;

import lombok.Data;

import java.util.List;

@Data
public class ThinkingModeRequest {
    private String topic;
    private String language;          // vi|en
    private String structureType;     // mindmap|logic|brace|org|tree|timeline|fishbone
    private Integer levels;           // depth levels
    private Integer firstLevelCount;  // number of first-level branches
    private List<String> tags;        // optional tags
    private String mode;              // normal|thinking|max
}

