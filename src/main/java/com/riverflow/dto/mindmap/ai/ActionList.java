package com.riverflow.dto.mindmap.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of Actions for the AGENT step.
 */
@Data
public class ActionList {
    private List<Action> actions = new ArrayList<>();

    public boolean isEmpty() {
        return actions == null || actions.isEmpty();
    }
}

