package com.riverflow.dto.mindmap.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for Thinking Mode - AI analyzes and optimizes user's raw prompt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThinkingModeRequest {

    @NotBlank(message = "userPrompt is required")
    @Size(max = 1000, message = "userPrompt must be <= 1000 chars")
    private String userPrompt;

    @Builder.Default
    private String language = "vi"; // vi/en

    // Optional: user can provide hints about what they want
    private List<String> tags;

    // Optional: preferred structure type
    private String preferredStructure;

    // Optional: complexity preference (simple, normal, detailed)
    @Builder.Default
    private String complexity = "normal";
}
