package com.riverflow.exception.mindmap;

/**
 * Exception thrown when a mindmap is not found
 */
public class MindmapNotFoundException extends RuntimeException {
    
    public MindmapNotFoundException(String message) {
        super(message);
    }
    
    public MindmapNotFoundException(String id, Long userId) {
        super(String.format("Mindmap with id '%s' not found for user %d", id, userId));
    }
}

