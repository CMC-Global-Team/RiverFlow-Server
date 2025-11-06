package com.riverflow.dto.mindmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for context node in AI Mindmap Request
 * Represents a node in the mindmap context
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextNode {
    
    /**
     * ID of the context node
     */
    private String id;
    
    /**
     * Summary/description of the context node
     */
    private String summary;
}

