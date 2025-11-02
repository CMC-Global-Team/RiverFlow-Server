package com.riverflow.model;

import lombok.*;

/**
 * Embedded document representing node formatting options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeFormat {
    
    private Integer fontSize = 14;
    private String fontFamily = "Arial";
    private String fontWeight = "normal";
    private String fontStyle = "normal";
    private String color = "#000000";
    private String backgroundColor = "#FFFFFF";
    private String borderColor = "#CCCCCC";
    private Integer borderWidth = 1;
    private Integer borderRadius = 4;
}

