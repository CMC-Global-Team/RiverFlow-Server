package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Edge label
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeLabel {
    private String text;
    private Double position = 0.5; // 0-1, position along the edge
}

