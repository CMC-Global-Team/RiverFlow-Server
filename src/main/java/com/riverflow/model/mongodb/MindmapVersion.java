package com.riverflow.model.mongodb;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Store major versions of mindmaps (snapshots)
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
    
    private Integer version; // Version number (incremental)
    
    private String name; // Optional version name/label
    
    private String description;
    
    private Map<String, Object> snapshot; // Complete snapshot of the mindmap
    
    private Long createdBy; // MySQL user ID who created this version
    
    private List<String> tags; // Version tags like "stable", "draft", "reviewed"
    
    private Boolean isAutoSave = false; // Whether this was an automatic save
    
    @Indexed
    private LocalDateTime createdAt;
}

