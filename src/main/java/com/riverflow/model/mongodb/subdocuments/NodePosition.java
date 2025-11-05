package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Node position in canvas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodePosition {
    private Double x = 0.0;
    private Double y = 0.0;
    private Integer z = 0; // Layer order
}

