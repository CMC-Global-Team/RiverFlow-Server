package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

/**
 * Node text formatting
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

