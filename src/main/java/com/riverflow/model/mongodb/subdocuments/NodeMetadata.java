package com.riverflow.model.mongodb.subdocuments;

import lombok.*;

import java.util.List;

/**
 * Additional node metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeMetadata {
    private String icon; // Icon name or URL
    private String image; // Image URL
    private String link; // External link
    private List<String> tags; // Tags for categorization
    private String notes; // Additional notes
    private Integer priority = 3; // 1-5
    private Boolean completed = false;
}

