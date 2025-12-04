package com.riverflow.dto.mindmap;

import lombok.Data;

/**
 * Request DTO for updating embed settings
 */
@Data
public class UpdateEmbedRequest {
    private Boolean isEmbedEnabled;
}
