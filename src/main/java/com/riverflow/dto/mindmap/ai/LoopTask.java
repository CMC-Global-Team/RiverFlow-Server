package com.riverflow.dto.mindmap.ai;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * One unit of work in Max Mode loop.
 */
@Data
public class LoopTask {
    private String id;
    private String title;
    private String description;
    private java.util.List<String> dependsOn;
    private String topic;
    private String structureType; // mindmap|logic|brace|org|tree|timeline|fishbone
    private Integer levels;
    private Integer firstLevelCount;
    private java.util.List<String> tags;
}

