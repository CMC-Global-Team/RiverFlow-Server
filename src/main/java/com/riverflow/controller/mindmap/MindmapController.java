package com.riverflow.controller.mindmap;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.MessageResponse;
import com.riverflow.dto.collaboration.InviteCollaboratorRequest;
import com.riverflow.dto.mindmap.CreateMindmapRequest;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.MindmapSummaryResponse;
import com.riverflow.dto.mindmap.UpdateMindmapRequest;
import com.riverflow.exception.mindmap.MindmapNotFoundException;
import com.riverflow.model.User;
import com.riverflow.model.mindmap.CollaborationInvitation;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.service.mindmap.CollaborationService;
import com.riverflow.service.mindmap.MindmapService;
import com.riverflow.service.mindmap.UndoRedoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for Mindmap CRUD operations
 */
@RestController
@RequestMapping("/mindmaps")
@RequiredArgsConstructor
@Slf4j
public class MindmapController {
    
    private final MindmapService mindmapService;
    private final CustomUserDetailsService userDetailsService;
    private final UndoRedoService undoRedoService;
    private final CollaborationService collaborationService;
    private final MindmapRepository mindmapRepository;

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
     * Nhân bản (duplicate) một mindmap
     * POST /api/mindmaps/{id}/duplicate
     */
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<MindmapResponse> duplicateMindmap(
            @PathVariable String id,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Duplicating mindmap: {} for user: {}", id, userId);

        MindmapResponse response = mindmapService.duplicateMindmap(id, userId);
        return ResponseEntity.ok(response);
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

    @PostMapping("/{id}/undo")
    public ResponseEntity<MindmapResponse> undo(
            @PathVariable String id,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Undoing mindmap: {} for user: {}", id, userId);

        MindmapResponse response = undoRedoService.undo(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/redo")
    public ResponseEntity<MindmapResponse> redo(
            @PathVariable String id,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Redoing mindmap: {} for user: {}", id, userId);

        MindmapResponse response = undoRedoService.redo(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/collaborators/invite")
    public ResponseEntity<CollaborationInvitation> inviteCollaborator(
            @PathVariable String id,
            @Valid @RequestBody InviteCollaboratorRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Request invite collaborator to map: {} by user: {}", id, userId);

        CollaborationInvitation invitation = collaborationService.inviteCollaborator(id, request, userId);

        return ResponseEntity.ok(invitation);
    }

    /**
     * Get all collaborators for a mindmap
     * GET /api/mindmaps/{id}/collaborators
     */
    @GetMapping("/{id}/collaborators")
    public ResponseEntity<?> getCollaborators(
            @PathVariable String id,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Getting collaborators for mindmap: {} by user: {}", id, userId);

        var response = collaborationService.getCollaborators(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update collaborator role
     * PUT /api/mindmaps/{id}/collaborators/{email}/role
     */
    @PutMapping("/{id}/collaborators/{email}/role")
    public ResponseEntity<?> updateCollaboratorRole(
            @PathVariable String id,
            @PathVariable String email,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Updating collaborator role for mindmap: {} by user: {}", id, userId);

        String role = request.get("role");
        var response = collaborationService.updateCollaboratorRole(id, email, role, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove collaborator from mindmap
     * DELETE /api/mindmaps/{id}/collaborators/{email}
     */
    @DeleteMapping("/{id}/collaborators/{email}")
    public ResponseEntity<MessageResponse> removeCollaborator(
            @PathVariable String id,
            @PathVariable String email,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("Removing collaborator from mindmap: {} by user: {}", id, userId);

        collaborationService.removeCollaborator(id, email, userId);
        return ResponseEntity.ok(new MessageResponse("Collaborator removed successfully"));
    }

    /**
     * Update public access level of mindmap
     * PUT /api/mindmaps/{id}/public-access
     */
    @PutMapping("/{id}/public-access")
    public ResponseEntity<MindmapResponse> updatePublicAccess(
            @PathVariable String id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        Boolean isPublic = (Boolean) request.get("isPublic");
        String accessLevel = (String) request.get("accessLevel");
        
        log.info("Updating public access for mindmap: {} by user: {}", id, userId);

        MindmapResponse response = mindmapService.updatePublicAccess(id, isPublic, accessLevel, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Accept collaboration invitation
     * POST /api/mindmaps/accept-invitation/{token}
     */
    @PostMapping("/accept-invitation/{token}")
    public ResponseEntity<MessageResponse> acceptInvitation(
            @PathVariable String token,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("User {} accepting invitation with token: {}", userId, token);

        collaborationService.acceptInvitation(token, userId);
        return ResponseEntity.ok(new MessageResponse("Invitation accepted successfully. Mindmap added to your collection."));
    }

    /**
     * Reject collaboration invitation
     * POST /api/mindmaps/reject-invitation/{token}
     */
    @PostMapping("/reject-invitation/{token}")
    public ResponseEntity<MessageResponse> rejectInvitation(
            @PathVariable String token,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        log.info("User {} rejecting invitation with token: {}", userId, token);

        collaborationService.rejectInvitation(token, userId);
        return ResponseEntity.ok(new MessageResponse("Invitation rejected successfully."));
    }

    /**
     * Verify collaboration invitation
     * GET /api/mindmaps/verify-invitation/{token}
     */
    @GetMapping("/verify-invitation/{token}")
    public ResponseEntity<?> verifyInvitation(@PathVariable String token) {
        log.info("Verifying invitation with token: {}", token);

        try {
            CollaborationInvitation invitation = collaborationService.getInvitationByToken(token);
            
            if (invitation == null || "expired".equals(invitation.getStatus())) {
                return ResponseEntity.status(400).body(new MessageResponse("Invitation has expired."));
            }

            if ("accepted".equals(invitation.getStatus())) {
                return ResponseEntity.status(400).body(new MessageResponse("Invitation has already been accepted."));
            }

            if ("rejected".equals(invitation.getStatus())) {
                return ResponseEntity.status(400).body(new MessageResponse("Invitation has been rejected."));
            }

            Mindmap mindmap = mindmapRepository.findById(invitation.getMindmapId())
                    .orElseThrow(() -> new MindmapNotFoundException("Mindmap not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mindmapId", mindmap.getId());
            response.put("mindmapTitle", mindmap.getTitle());
            response.put("invitedBy", invitation.getInvitedByUserId());
            response.put("expiresAt", invitation.getExpiresAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to verify invitation: {}", e.getMessage());
            return ResponseEntity.status(400).body(new MessageResponse("Invalid or expired invitation."));
        }
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

