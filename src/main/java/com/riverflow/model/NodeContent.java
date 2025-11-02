package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing node content
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeContent {
    
    private String text;
    private String html; // Rich text content
    private NodeFormat format;
}

