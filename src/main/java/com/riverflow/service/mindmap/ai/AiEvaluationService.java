package com.riverflow.service.mindmap.ai;

import com.riverflow.dto.mindmap.ai.ActionList;
import com.riverflow.dto.mindmap.ai.EvaluationResult;
import com.riverflow.model.mindmap.Mindmap;

/**
 * Strong evaluation & refinement service for MaxMode.
 * This service is responsible for:
 * - multi-criteria quality evaluation of a mindmap
 * - producing refinement actions when quality is below target.
 */
public interface AiEvaluationService {

    /**
     * Evaluate current mindmap against the intended design / prompt.
     *
     * @param mindmap        current mindmap state
     * @param originalPrompt original user prompt
     * @param language       target language (vi, en, ...)
     * @param structureType  desired structure (mindmap, logic, ...)
     * @param levels         desired depth
     * @param firstLevelCount desired first-level branch count
     * @return evaluation result with score and issues
     */
    EvaluationResult evaluate(Mindmap mindmap,
                              String originalPrompt,
                              String language,
                              String structureType,
                              Integer levels,
                              Integer firstLevelCount);

    /**
     * When evaluation score is below target, ask AI to propose concrete fix actions.
     *
     * @param mindmap         current mindmap
     * @param evaluation      result returned by {@link #evaluate}
     * @param language        language for any rationale text
     * @return list of actions to run through AiOperationExecutor
     */
    ActionList refine(Mindmap mindmap,
                      EvaluationResult evaluation,
                      String language);
}


