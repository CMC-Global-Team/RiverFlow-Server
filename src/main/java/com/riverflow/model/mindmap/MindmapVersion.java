package com.riverflow.model.mindmap;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mindmap version snapshots
 */
@Document(collection = "mindmap_versions")
@CompoundIndex(name = "mindmap_version_unique", def = "{'mindmapId': 1, 'version': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapVersion {
    
    @Id
    private String id;
    
    @Indexed
    private String mindmapId;
    
    private Integer version;
    
    private String name;
    
    private String description;
    
    private Map<String, Object> snapshot; // Full mindmap snapshot
    
    private Long createdBy;
    
    private List<String> tags;
    
    @Builder.Default
    private Boolean isAutoSave = false;
    
    @Indexed
    private LocalDateTime createdAt;
}

