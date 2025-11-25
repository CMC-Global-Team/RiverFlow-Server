package com.riverflow.dto.mindmap.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateMindmapRequest {

    @NotBlank(message = "topic is required")
    @Size(max = 200, message = "topic must be <= 200 chars")
    private String topic;

    @Size(max = 200, message = "title must be <= 200 chars")
    private String title; // optional; if not provided, use topic

    @Builder.Default
    @Min(1)
    @Max(3)
    private Integer levels = 2; // depth (root + n levels)

    @Builder.Default
    @Min(3)
    @Max(6)
    private Integer firstLevelCount = 5; // number of level-1 nodes (range depends on mode)

    @Builder.Default
    private String language = "vi"; // vi/en

    // Processing mode: normal | (future: deep, creative)
    @Builder.Default
    private String mode = "normal";

    // Optional tags to bias generation
    private List<String> tags;

    // Structure type: mindmap | logic | brace | org | tree | timeline | fishbone
    @Builder.Default
    private String structureType = "mindmap";
}
