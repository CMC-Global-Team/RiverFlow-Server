package com.riverflow.dto.mindmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ReactFlow viewport settings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewportDto {
    
    private Double x;
    
    private Double y;
    
    private Double zoom;
}

