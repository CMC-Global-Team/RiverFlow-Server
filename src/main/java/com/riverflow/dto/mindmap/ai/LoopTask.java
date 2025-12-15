package com.riverflow.dto.mindmap.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Single task in an AI loop plan.
 */
@Data
public class LoopTask {
    private String id;
    private String topic;
    private String structureType;
    private Integer levels;
    private Integer firstLevelCount;
    private List<String> tags = new ArrayList<>();
    private List<String> dependsOn = new ArrayList<>();
}

