package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Viewport information in real-time session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Viewport {
    private Double x;
    private Double y;
    private Double zoom;
}

