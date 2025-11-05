package com.riverflow.util.mindmap;

import com.riverflow.dto.mindmap.*;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.model.mindmap.subdocuments.MindmapSettings;
import com.riverflow.model.mindmap.subdocuments.Viewport;

/**
 * Mapper utility for converting between Mindmap entities and DTOs
 */
public class MindmapMapper {
    
    /**
     * Convert Mindmap entity to MindmapResponse DTO
     */
    public static MindmapResponse toResponse(Mindmap mindmap) {
        if (mindmap == null) {
            return null;
        }
        
        return MindmapResponse.builder()
                .id(mindmap.getId())
                .mysqlUserId(mindmap.getMysqlUserId())
                .title(mindmap.getTitle())
                .description(mindmap.getDescription())
                .thumbnail(mindmap.getThumbnail())
                .nodes(mindmap.getNodes())
                .edges(mindmap.getEdges())
                .viewport(toViewportDto(mindmap.getViewport()))
                .settings(toSettingsDto(mindmap.getSettings()))
                .isPublic(mindmap.getIsPublic())
                .shareToken(mindmap.getShareToken())
                .collaborators(mindmap.getCollaborators())
                .tags(mindmap.getTags())
                .category(mindmap.getCategory())
                .isFavorite(mindmap.getIsFavorite())
                .isTemplate(mindmap.getIsTemplate())
                .status(mindmap.getStatus())
                .aiGenerated(mindmap.getAiGenerated())
                .aiWorkflowId(mindmap.getAiWorkflowId())
                .aiMetadata(mindmap.getAiMetadata())
                .metadata(mindmap.getMetadata())
                .createdAt(mindmap.getCreatedAt())
                .updatedAt(mindmap.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert Mindmap entity to MindmapSummaryResponse DTO
     */
    public static MindmapSummaryResponse toSummaryResponse(Mindmap mindmap) {
        if (mindmap == null) {
            return null;
        }
        
        return MindmapSummaryResponse.builder()
                .id(mindmap.getId())
                .mysqlUserId(mindmap.getMysqlUserId())
                .title(mindmap.getTitle())
                .description(mindmap.getDescription())
                .thumbnail(mindmap.getThumbnail())
                .nodeCount(mindmap.getNodes() != null ? mindmap.getNodes().size() : 0)
                .edgeCount(mindmap.getEdges() != null ? mindmap.getEdges().size() : 0)
                .tags(mindmap.getTags())
                .category(mindmap.getCategory())
                .isFavorite(mindmap.getIsFavorite())
                .isTemplate(mindmap.getIsTemplate())
                .isPublic(mindmap.getIsPublic())
                .status(mindmap.getStatus())
                .aiGenerated(mindmap.getAiGenerated())
                .aiWorkflowId(mindmap.getAiWorkflowId())
                .createdAt(mindmap.getCreatedAt())
                .updatedAt(mindmap.getUpdatedAt())
                .build();
    }
    
    /**
     * Convert Viewport entity to ViewportDto
     */
    private static ViewportDto toViewportDto(Viewport viewport) {
        if (viewport == null) {
            return null;
        }
        
        return ViewportDto.builder()
                .x(viewport.getX())
                .y(viewport.getY())
                .zoom(viewport.getZoom())
                .build();
    }
    
    /**
     * Convert ViewportDto to Viewport entity
     */
    public static Viewport toViewportEntity(ViewportDto dto) {
        if (dto == null) {
            return null;
        }
        
        return Viewport.builder()
                .x(dto.getX())
                .y(dto.getY())
                .zoom(dto.getZoom())
                .build();
    }
    
    /**
     * Convert MindmapSettings entity to MindmapSettingsDto
     */
    private static MindmapSettingsDto toSettingsDto(MindmapSettings settings) {
        if (settings == null) {
            return null;
        }
        
        return MindmapSettingsDto.builder()
                .fitView(settings.getFitView())
                .snapToGrid(settings.getSnapToGrid())
                .snapGrid(settings.getSnapGrid())
                .nodesDraggable(settings.getNodesDraggable())
                .nodesConnectable(settings.getNodesConnectable())
                .elementsSelectable(settings.getElementsSelectable())
                .panOnDrag(settings.getPanOnDrag())
                .panOnScroll(settings.getPanOnScroll())
                .zoomOnScroll(settings.getZoomOnScroll())
                .zoomOnPinch(settings.getZoomOnPinch())
                .zoomOnDoubleClick(settings.getZoomOnDoubleClick())
                .defaultEdgeOptions(settings.getDefaultEdgeOptions())
                .connectionMode(settings.getConnectionMode())
                .build();
    }
    
    /**
     * Convert MindmapSettingsDto to MindmapSettings entity
     */
    public static MindmapSettings toSettingsEntity(MindmapSettingsDto dto) {
        if (dto == null) {
            return null;
        }
        
        return MindmapSettings.builder()
                .fitView(dto.getFitView())
                .snapToGrid(dto.getSnapToGrid())
                .snapGrid(dto.getSnapGrid())
                .nodesDraggable(dto.getNodesDraggable())
                .nodesConnectable(dto.getNodesConnectable())
                .elementsSelectable(dto.getElementsSelectable())
                .panOnDrag(dto.getPanOnDrag())
                .panOnScroll(dto.getPanOnScroll())
                .zoomOnScroll(dto.getZoomOnScroll())
                .zoomOnPinch(dto.getZoomOnPinch())
                .zoomOnDoubleClick(dto.getZoomOnDoubleClick())
                .defaultEdgeOptions(dto.getDefaultEdgeOptions())
                .connectionMode(dto.getConnectionMode())
                .build();
    }
}

