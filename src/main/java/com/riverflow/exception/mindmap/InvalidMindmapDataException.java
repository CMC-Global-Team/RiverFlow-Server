package com.riverflow.exception.mindmap;

/**
 * Exception thrown when mindmap data is invalid
 */
public class InvalidMindmapDataException extends RuntimeException {
    
    public InvalidMindmapDataException(String message) {
        super(message);
    }
    
    public InvalidMindmapDataException(String field, String reason) {
        super(String.format("Invalid mindmap data for field '%s': %s", field, reason));
    }
}

