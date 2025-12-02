package com.riverflow.dto.mindmap.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizeRequest {

    @NotBlank(message = "mindmapId is required")
    private String mindmapId;

    // node | description
    @NotBlank(message = "targetType is required")
    @Pattern(regexp = "node|description|structure|auto", message = "targetType must be 'node' or 'description' or 'structure' or 'auto'")
    @Builder.Default
    private String targetType = "auto";

    // required when targetType = node
    private String nodeId;

    @Builder.Default
    private String language = "vi"; // vi/en

    @Builder.Default
    private String mode = "normal"; // normal (fast, concise)

    // Optional guidance to bias optimization
    private List<String> hints;

    private Integer levels;
    private Integer firstLevelCount;
    private List<String> tags;

    private String structureType; // mindmap, logic, brace, org, tree, timeline, fishbone
}


