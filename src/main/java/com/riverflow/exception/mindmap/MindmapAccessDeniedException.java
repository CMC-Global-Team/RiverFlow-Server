package com.riverflow.exception.mindmap;

/**
 * Exception thrown when user doesn't have permission to access a mindmap
 */
public class MindmapAccessDeniedException extends RuntimeException {
    
    public MindmapAccessDeniedException(String message) {
        super(message);
    }
    
    public MindmapAccessDeniedException(String mindmapId, Long userId) {
        super(String.format("User %d does not have permission to access mindmap '%s'", userId, mindmapId));
    }
}

