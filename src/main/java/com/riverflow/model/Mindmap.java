package com.riverflow.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB document representing a mindmap
 * Links to MySQL user via mysqlUserId field
 */
@Document(collection = "mindmaps")
@CompoundIndexes({
    @CompoundIndex(name = "idx_user_status", def = "{'mysqlUserId': 1, 'status': 1}"),
    @CompoundIndex(name = "idx_public_status", def = "{'isPublic': 1, 'status': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mindmap {
    
    @Id
    private String id; // MongoDB ObjectId
    
    /**
     * Owner user ID from MySQL database
     */
    @Indexed
    private Long mysqlUserId;
    
    /**
     * Mindmap title
     */
    @TextIndexed
    @Indexed
    private String title;
    
    /**
     * Mindmap description
     */
    @TextIndexed
    private String description;
    
    /**
     * URL to mindmap thumbnail image
     */
    private String thumbnail;
    
    /**
     * Array of nodes in the mindmap
     */
    @Builder.Default
    private List<Node> nodes = new ArrayList<>();
    
    /**
     * Array of edges connecting nodes
     */
    @Builder.Default
    private List<Edge> edges = new ArrayList<>();
    
    /**
     * Mindmap settings (theme, layout, etc.)
     */
    private MindmapSettings settings;
    
    /**
     * Whether mindmap is publicly accessible
     */
    @Indexed
    private Boolean isPublic = false;
    
    /**
     * Token for public sharing link
     */
    @Indexed(unique = true, sparse = true)
    private String shareToken;
    
    /**
     * Users who have access to this mindmap
     */
    @Builder.Default
    private List<Collaborator> collaborators = new ArrayList<>();
    
    /**
     * Tags for categorization
     */
    @Indexed
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    
    /**
     * Mindmap category
     */
    private MindmapCategory category = MindmapCategory.OTHER;
    
    /**
     * Marked as favorite by owner
     */
    private Boolean isFavorite = false;
    
    /**
     * Can be used as a template
     */
    private Boolean isTemplate = false;
    
    /**
     * Mindmap status
     */
    @Indexed
    private MindmapStatus status = MindmapStatus.ACTIVE;
    
    /**
     * Additional metadata
     */
    private MindmapMetadata metadata;
    
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Indexed
    private LocalDateTime updatedAt;
    
    // Enums
    
    public enum MindmapCategory {
        WORK,
        PERSONAL,
        EDUCATION,
        PROJECT,
        BRAINSTORMING,
        OTHER
    }
    
    public enum MindmapStatus {
        ACTIVE,
        ARCHIVED,
        DELETED
    }
}

