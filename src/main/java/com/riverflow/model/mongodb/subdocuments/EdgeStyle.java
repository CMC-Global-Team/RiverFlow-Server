package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Edge visual style
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeStyle {
    private String strokeColor = "#999999";
    private Integer strokeWidth = 2;
    private String strokeStyle = "solid"; // solid, dashed, dotted
}

