package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Cursor position in real-time session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cursor {
    private Double x;
    private Double y;
    private String nodeId; // Currently selected/editing node
}

