package com.riverflow.dto.mindmap;

import lombok.Data;

import java.util.Map;

@Data
public class LogHistoryRequest {
    private String action;
    private Map<String, Object> changes;
    private Map<String, Object> snapshot;
    private Map<String, Object> metadata;
    private String status;
}

