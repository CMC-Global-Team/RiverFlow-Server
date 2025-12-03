package com.riverflow.dto.mindmap.ai;

import lombok.Data;

import java.util.Map;

/**
 * OTMZ (Optimized Thinking Model JSON)
 * Minimal, map-based structure for fast iteration.
 */
@Data
public class Otmz {
    private Map<String, Object> meta;
    private Map<String, Object> promptAnalysis;
    private Map<String, Object> propertiesDesign;
    private Map<String, Object> optimizedContent;
}

