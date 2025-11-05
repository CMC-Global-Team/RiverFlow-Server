package com.riverflow.model.mongodb;

import com.riverflow.model.mongodb.subdocuments.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Track active real-time editing sessions
 * Used for showing who's online and cursor positions
 */
@Document(collection = "realtime_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RealtimeSession {
    
    @Id
    private String id;
    
    @Indexed
    private String mindmapId;
    
    @Indexed
    private Long mysqlUserId;
    
    @Indexed
    private String socketId; // Socket.IO connection ID
    
    private UserInfo userInfo;
    
    private Cursor cursor;
    
    private Viewport viewport;
    
    @Indexed
    private Boolean isActive = true;
    
    private Boolean isEditing = false; // Currently making changes
    
    @Indexed
    private LocalDateTime lastActivity;
    
    private LocalDateTime connectedAt;
}

