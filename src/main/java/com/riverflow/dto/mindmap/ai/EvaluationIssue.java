package com.riverflow.dto.mindmap.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Single quality issue detected during AI evaluation.
 */
@Data
public class EvaluationIssue {

    /**
     * Machine-friendly type, e.g.
     * DUPLICATE_BRANCH, DEPTH_IMBALANCE, OFF_TOPIC, WEAK_LEAF, STRUCTURE_MISMATCH
     */
    private String type;

    /**
     * human-readable, localized description.
     */
    private String message;

    /**
     * optional severity: info|warning|error.
     */
    private String severity;

    /**
     * affected node ids (if any).
     */
    private List<String> nodeIds = new ArrayList<>();

    /**
     * optional suggestion for how to fix this issue.
     */
    private String suggestion;
}


