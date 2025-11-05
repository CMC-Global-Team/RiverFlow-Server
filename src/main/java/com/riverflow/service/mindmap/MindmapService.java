package com.riverflow.service.mindmap;

import com.riverflow.dto.mindmap.CreateMindmapRequest;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.MindmapSummaryResponse;
import com.riverflow.dto.mindmap.UpdateMindmapRequest;

import java.util.List;

/**
 * Service interface for Mindmap operations
 */
public interface MindmapService {
    
    /**
     * Create a new mindmap
     */
    MindmapResponse createMindmap(CreateMindmapRequest request, Long userId);
    
    /**
     * Get mindmap by ID
     */
    MindmapResponse getMindmapById(String mindmapId, Long userId);
    
    /**
     * Update an existing mindmap
     */
    MindmapResponse updateMindmap(String mindmapId, UpdateMindmapRequest request, Long userId);
    
    /**
     * Delete a mindmap (soft delete - set status to 'deleted')
     */
    void deleteMindmap(String mindmapId, Long userId);
    
    /**
     * Permanently delete a mindmap
     */
    void permanentlyDeleteMindmap(String mindmapId, Long userId);
    
    /**
     * Get all mindmaps for a user
     */
    List<MindmapSummaryResponse> getAllMindmapsByUser(Long userId);
    
    /**
     * Get mindmaps by category
     */
    List<MindmapSummaryResponse> getMindmapsByCategory(Long userId, String category);
    
    /**
     * Get favorite mindmaps
     */
    List<MindmapSummaryResponse> getFavoriteMindmaps(Long userId);
    
    /**
     * Get archived mindmaps
     */
    List<MindmapSummaryResponse> getArchivedMindmaps(Long userId);
    
    /**
     * Toggle favorite status
     */
    MindmapResponse toggleFavorite(String mindmapId, Long userId);
    
    /**
     * Archive a mindmap
     */
    MindmapResponse archiveMindmap(String mindmapId, Long userId);
    
    /**
     * Unarchive a mindmap
     */
    MindmapResponse unarchiveMindmap(String mindmapId, Long userId);
    
    /**
     * Search mindmaps by title or description
     */
    List<MindmapSummaryResponse> searchMindmaps(Long userId, String keyword);
}

