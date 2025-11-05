package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Node size
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeSize {
    private Double width = 150.0;
    private Double height = 50.0;
}

