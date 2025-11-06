package com.riverflow.dto.mindmap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for AI Mindmap Assistant
 * 
 * Expected JSON format:
 * {
 *   "node_title": "...",
 *   "node_summary": "...",
 *   "context_nodes": [{"id":"...","summary":"..."}],
 *   "user_instruction": "..."
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMindmapRequest {
    
    /**
     * Title of the current node
     */
    @JsonProperty("node_title")
    private String nodeTitle;
    
    /**
     * Summary of the current node
     */
    @JsonProperty("node_summary")
    private String nodeSummary;
    
    /**
     * Context nodes for understanding the mindmap structure
     * Format: [{"id":"...","summary":"..."}]
     */
    @JsonProperty("context_nodes")
    private List<ContextNode> contextNodes;
    
    /**
     * User instruction: expand, summarize, add new idea, restructure, etc.
     */
    @JsonProperty("user_instruction")
    private String userInstruction;
}

