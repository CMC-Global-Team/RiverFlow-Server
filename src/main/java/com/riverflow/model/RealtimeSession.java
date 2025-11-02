package com.riverflow.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * MongoDB document for tracking active real-time editing sessions
 * Used for showing who's online and cursor positions
 */
@Document(collection = "realtime_sessions")
@CompoundIndexes({
    @CompoundIndex(name = "idx_mindmap_active", def = "{'mindmapId': 1, 'isActive': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealtimeSession {
    
    @Id
    private String id;
    
    /**
     * Reference to the mindmap
     */
    @Indexed
    private String mindmapId;
    
    /**
     * MySQL user ID
     */
    @Indexed
    private Long mysqlUserId;
    
    /**
     * Socket.IO connection ID
     */
    @Indexed(unique = true)
    private String socketId;
    
    /**
     * User information
     */
    private UserInfo userInfo;
    
    /**
     * Cursor position
     */
    private CursorPosition cursor;
    
    /**
     * Viewport information
     */
    private Viewport viewport;
    
    /**
     * Whether session is active
     */
    @Indexed
    private Boolean isActive = true;
    
    /**
     * Currently making changes
     */
    private Boolean isEditing = false;
    
    /**
     * Last activity timestamp
     */
    @Indexed
    private LocalDateTime lastActivity = LocalDateTime.now();
    
    /**
     * Connection timestamp
     */
    @Field("connectedAt")
    private LocalDateTime connectedAt = LocalDateTime.now();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private String email;
        private String fullName;
        private String avatar;
        private String color; // Assigned color for cursor and selections
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CursorPosition {
        private Double x;
        private Double y;
        private String nodeId; // Currently selected/editing node
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Viewport {
        private Double x;
        private Double y;
        private Double zoom;
    }
}

