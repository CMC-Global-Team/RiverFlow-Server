package com.riverflow.controller.mindmap;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.mindmap.AiMindmapRequest;
import com.riverflow.dto.mindmap.AiMindmapResponse;
import com.riverflow.dto.mindmap.CreateMindmapRequest;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.MindmapSummaryResponse;
import com.riverflow.dto.mindmap.UpdateMindmapRequest;
import com.riverflow.model.User;
import com.riverflow.service.mindmap.AiMindmapService;
import com.riverflow.service.mindmap.MindmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Mindmap CRUD operations
 */
@RestController
@RequestMapping("/mindmaps")
@RequiredArgsConstructor
@Slf4j
public class MindmapController {
    
    private final MindmapService mindmapService;
    private final AiMindmapService aiMindmapService;
    private final CustomUserDetailsService userDetailsService;
    
    /**
     * Create a new mindmap
     * POST /api/mindmaps
     */
    @PostMapping
    public ResponseEntity<MindmapResponse> createMindmap(
            @Valid @RequestBody CreateMindmapRequest request,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Creating mindmap for user: {}", userId);
        
        MindmapResponse response = mindmapService.createMindmap(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get all mindmaps for current user
     * GET /api/mindmaps
     */
    @GetMapping
    public ResponseEntity<List<MindmapSummaryResponse>> getAllMindmaps(
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Getting all mindmaps for user: {}", userId);
        
        List<MindmapSummaryResponse> response = mindmapService.getAllMindmapsByUser(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get mindmap by ID
     * GET /api/mindmaps/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MindmapResponse> getMindmapById(
            @PathVariable String id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Getting mindmap: {} for user: {}", id, userId);
        
        MindmapResponse response = mindmapService.getMindmapById(id, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update mindmap
     * PUT /api/mindmaps/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<MindmapResponse> updateMindmap(
            @PathVariable String id,
            @Valid @RequestBody UpdateMindmapRequest request,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Updating mindmap: {} for user: {}", id, userId);
        
        MindmapResponse response = mindmapService.updateMindmap(id, request, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete mindmap (soft delete)
     * DELETE /api/mindmaps/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteMindmap(
            @PathVariable String id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Deleting mindmap: {} for user: {}", id, userId);
        
        mindmapService.deleteMindmap(id, userId);
        return ResponseEntity.ok(new MessageResponse("Mindmap deleted successfully"));
    }
    
    /**
     * Permanently delete mindmap
     * DELETE /api/mindmaps/{id}/permanent
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<MessageResponse> permanentlyDeleteMindmap(
            @PathVariable String id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Permanently deleting mindmap: {} for user: {}", id, userId);
        
        mindmapService.permanentlyDeleteMindmap(id, userId);
        return ResponseEntity.ok(new MessageResponse("Mindmap permanently deleted"));
    }
    
    /**
     * Get mindmaps by category
     * GET /api/mindmaps/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<MindmapSummaryResponse>> getMindmapsByCategory(
            @PathVariable String category,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Getting mindmaps by category: {} for user: {}", category, userId);
        
        List<MindmapSummaryResponse> response = mindmapService.getMindmapsByCategory(userId, category);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get favorite mindmaps
     * GET /api/mindmaps/favorites
     */
    @GetMapping("/favorites")
    public ResponseEntity<List<MindmapSummaryResponse>> getFavoriteMindmaps(
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Getting favorite mindmaps for user: {}", userId);
        
        List<MindmapSummaryResponse> response = mindmapService.getFavoriteMindmaps(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get archived mindmaps
     * GET /api/mindmaps/archived
     */
    @GetMapping("/archived")
    public ResponseEntity<List<MindmapSummaryResponse>> getArchivedMindmaps(
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Getting archived mindmaps for user: {}", userId);
        
        List<MindmapSummaryResponse> response = mindmapService.getArchivedMindmaps(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Toggle favorite status
     * POST /api/mindmaps/{id}/toggle-favorite
     */
    @PostMapping("/{id}/toggle-favorite")
    public ResponseEntity<MindmapResponse> toggleFavorite(
            @PathVariable String id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Toggling favorite for mindmap: {} user: {}", id, userId);
        
        MindmapResponse response = mindmapService.toggleFavorite(id, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Archive mindmap
     * POST /api/mindmaps/{id}/archive
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<MindmapResponse> archiveMindmap(
            @PathVariable String id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Archiving mindmap: {} for user: {}", id, userId);
        
        MindmapResponse response = mindmapService.archiveMindmap(id, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Unarchive mindmap
     * POST /api/mindmaps/{id}/unarchive
     */
    @PostMapping("/{id}/unarchive")
    public ResponseEntity<MindmapResponse> unarchiveMindmap(
            @PathVariable String id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Unarchiving mindmap: {} for user: {}", id, userId);
        
        MindmapResponse response = mindmapService.unarchiveMindmap(id, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Search mindmaps
     * GET /api/mindmaps/search?keyword=...
     */
    @GetMapping("/search")
    public ResponseEntity<List<MindmapSummaryResponse>> searchMindmaps(
            @RequestParam String keyword,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("Searching mindmaps for user: {} with keyword: {}", userId, keyword);
        
        List<MindmapSummaryResponse> response = mindmapService.searchMindmaps(userId, keyword);
        return ResponseEntity.ok(response);
    }
    
    /**
     * AI Mindmap Assistant
     * POST /api/mindmaps/ai/assist
     * 
     * Processes AI requests for mindmap operations:
     * - Expand nodes: adds 2-4 child nodes
     * - Summarize: creates summary structure
     * - Add new ideas: adds new nodes based on instruction
     * - Restructure: reorganizes mindmap structure
     * 
     * Request body:
     * {
     *   "node_title": "...",
     *   "node_summary": "...",
     *   "context_nodes": [{"id":"...","summary":"..."}],
     *   "user_instruction": "..."
     * }
     * 
     * Response: JSON with nodes and edges in React Flow format
     */
    @PostMapping("/ai/assist")
    public ResponseEntity<AiMindmapResponse> aiAssist(
            @Valid @RequestBody AiMindmapRequest request,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuth(authentication);
        log.info("AI assist request from user: {} - instruction: {}", 
            userId, request.getUserInstruction());
        
        AiMindmapResponse response = aiMindmapService.processAiRequest(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Helper method to get user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String email = authentication.getName();
        User user = userDetailsService.loadUserEntityByEmail(email);
        return user.getId();
    }
}

