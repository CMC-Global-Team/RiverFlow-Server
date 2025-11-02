package com.riverflow.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MongoDB document for storing major versions of mindmaps (snapshots)
 */
@Document(collection = "mindmap_versions")
@CompoundIndexes({
    @CompoundIndex(name = "idx_mindmap_version", def = "{'mindmapId': 1, 'version': -1}", unique = true),
    @CompoundIndex(name = "idx_mindmap_created", def = "{'mindmapId': 1, 'createdAt': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MindmapVersion {
    
    @Id
    private String id;
    
    /**
     * Reference to the mindmap
     */
    @Indexed
    private String mindmapId;
    
    /**
     * Version number (incremental)
     */
    private Integer version;
    
    /**
     * Optional version name/label
     */
    private String name;
    
    /**
     * Version description
     */
    private String description;
    
    /**
     * Complete snapshot of the mindmap at this version
     */
    private Map<String, Object> snapshot;
    
    /**
     * User who created this version
     */
    private Long createdBy;
    
    /**
     * Version tags like "stable", "draft", "reviewed"
     */
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    /**
     * Whether this was an automatic save
     */
    private Boolean isAutoSave = false;
    
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;
}

