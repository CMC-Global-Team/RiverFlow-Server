package com.riverflow.dto.mindmap.ai;

import lombok.Data;

/**
 * Planner result for Max Mode loop.
 */
@Data
public class LoopPlan {
    private Integer iterations;
    private java.util.List<LoopTask> tasks = new java.util.ArrayList<>();
}
