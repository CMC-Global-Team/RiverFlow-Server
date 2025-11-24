package com.riverflow.exception.mindmap;

/**
 * Exception thrown when user doesn't have permission to access a mindmap
 */
public class MindmapAccessDeniedException extends RuntimeException {
    private String mindmapId;
    private String shareToken;
    
    public MindmapAccessDeniedException(String message, String mindmapId, Long userId) {
        super(message);
        this.mindmapId = mindmapId;
    }
    
    public MindmapAccessDeniedException(String mindmapId, Long userId) {
        super(String.format("User %d does not have permission to access mindmap '%s'", userId, mindmapId));
        this.mindmapId = mindmapId;
    }
    
    public MindmapAccessDeniedException(String mindmapId, Long userId, String shareToken) {
        super(String.format("User %d does not have permission to access mindmap '%s'", userId, mindmapId));
        this.mindmapId = mindmapId;
        this.shareToken = shareToken;
    }
    
    public String getMindmapId() {
        return mindmapId;
    }
    
    public String getShareToken() {
        return shareToken;
    }
}

