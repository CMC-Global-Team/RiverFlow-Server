package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Canvas size
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanvasSize {
    private Double width = 5000.0;
    private Double height = 5000.0;
}

