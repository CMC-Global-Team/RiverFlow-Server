package com.riverflow.model.mindmap;

import com.riverflow.model.mindmap.subdocuments.Cursor;
import com.riverflow.model.mindmap.subdocuments.UserInfo;
import com.riverflow.model.mindmap.subdocuments.Viewport;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Active real-time editing sessions
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
    @Builder.Default
    private Boolean isActive = true;
    
    @Builder.Default
    private Boolean isEditing = false;
    
    @Indexed
    private LocalDateTime lastActivity;
    
    private LocalDateTime connectedAt;
}

