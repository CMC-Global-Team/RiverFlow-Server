package com.riverflow.dto.mindmap.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of evaluating a mindmap in MaxMode.
 */
@Data
public class EvaluationResult {

    /**
     * Overall quality score in range [0,1].
     */
    private Double score;

    /**
     * Detailed issues discovered.
     */
    private List<EvaluationIssue> issues = new ArrayList<>();

    /**
     * Short natural-language summary for UI (localized).
     */
    private String summary;

    /**
     * Optional action-level suggestions description.
     * Concrete operations will be delivered via ActionList from refinement.
     */
    private String refinementHint;
}


