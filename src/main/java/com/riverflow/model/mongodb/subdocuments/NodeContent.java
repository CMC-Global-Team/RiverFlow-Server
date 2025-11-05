package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Node content with formatting
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

