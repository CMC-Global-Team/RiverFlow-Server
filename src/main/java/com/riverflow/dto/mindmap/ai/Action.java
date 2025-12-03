package com.riverflow.dto.mindmap.ai;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Action item for AGENT step.
 * Type examples: add_node, update_node, delete_node, delete_subtree, set_title, set_structureType
 */
@Data
public class Action {
    /** Optional identifier for dependency chains */
    private String id;

    /** The action type (e.g., add_node) */
    private String type;

    /**
     * Free-form parameters for the action, e.g.
     * { parentLabel, label, description, shape, color, background, icon }
     */
    private Map<String, Object> params;

    /** Short reason for traceability */
    private String rationale;

    /** Lower number = higher priority (optional) */
    private Integer priority;

    /** Actions this action depends on (by id) */
    private List<String> dependsOn;
}

