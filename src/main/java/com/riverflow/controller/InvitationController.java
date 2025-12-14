package com.riverflow.controller;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.invitation.InvitationResponse;
import com.riverflow.model.User;
import com.riverflow.model.mindmap.subdocuments.Collaborator;
import com.riverflow.model.mindmap.CollaborationInvitation;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.repository.UserRepository;
import com.riverflow.repository.mindmap.CollaborationInvitationRepository;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.service.mindmap.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for public invitation endpoints (no auth required for email links)
 */
@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final CollaborationService collaborationService;
    private final CollaborationInvitationRepository invitationRepository;
    private final MindmapRepository mindmapRepository;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Get invitation details by token (public endpoint)
     * Used by modal to display invitation info
     * GET /api/invitations/{token}
     */
    @GetMapping("/{token}")
    public ResponseEntity<?> getInvitationDetails(@PathVariable String token) {
        try {
            CollaborationInvitation invitation = invitationRepository.findByToken(token)
                    .orElse(null);

            if (invitation == null) {
                return ResponseEntity.status(404).body(
                        Map.of("success", false, "message", "Invitation not found"));
            }

            if ("expired".equals(invitation.getStatus()) ||
                    (invitation.getExpiresAt() != null &&
                            invitation.getExpiresAt().isBefore(java.time.LocalDateTime.now()))) {
                return ResponseEntity.status(400).body(
                        Map.of("success", false, "message", "Invitation has expired"));
            }

            Mindmap mindmap = mindmapRepository.findById(invitation.getMindmapId())
                    .orElse(null);

            User inviter = userRepository.findById(invitation.getInvitedByUserId())
                    .orElse(null);

            InvitationResponse response = InvitationResponse.builder()
                    .id(invitation.getId())
                    .token(invitation.getToken())
                    .mindmapId(invitation.getMindmapId())
                    .mindmapTitle(mindmap != null ? mindmap.getTitle() : "Unknown")
                    .mindmapDescription(mindmap != null ? mindmap.getDescription() : null)
                    .invitedByUserId(invitation.getInvitedByUserId())
                    .inviterName(inviter != null ? inviter.getFullName() : "Someone")
                    .inviterEmail(inviter != null ? inviter.getEmail() : null)
                    .inviterAvatarUrl(null)
                    .invitedEmail(invitation.getInvitedEmail())
                    .role(invitation.getRole() != null ? invitation.getRole().toString() : "VIEWER")
                    .status(invitation.getStatus())
                    .message(invitation.getMessage())
                    .createdAt(invitation.getCreatedAt())
                    .expiresAt(invitation.getExpiresAt())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching invitation details", e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "Error fetching invitation details"));
        }
    }

    /**
     * Accept invitation via email link (public, no auth required)
     * POST /api/invitations/{token}/accept
     */
    @PostMapping("/{token}/accept")
    public ResponseEntity<?> acceptInvitationPublic(
            @PathVariable String token,
            Authentication authentication) {

        try {
            Long userId = null;
            if (authentication != null) {
                try {
                    String email = authentication.getName();
                    User user = userDetailsService.loadUserEntityByEmail(email);
                    userId = user.getId();
                } catch (Exception e) {
                    // User not authenticated, will handle below
                }
            }

            if (userId != null) {
                // Authenticated user - use standard flow
                collaborationService.acceptInvitation(token, userId);

                CollaborationInvitation invitation = collaborationService.getInvitationByToken(token);
                Mindmap mindmap = mindmapRepository.findById(invitation.getMindmapId()).orElse(null);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Invitation accepted successfully!");
                response.put("mindmapId", mindmap != null ? mindmap.getId() : null);
                response.put("mindmapTitle", mindmap != null ? mindmap.getTitle() : null);

                return ResponseEntity.ok(response);
            } else {
                // Non-authenticated user - accept via token only
                CollaborationInvitation invitation = invitationRepository.findByToken(token)
                        .orElse(null);

                if (invitation == null) {
                    return ResponseEntity.status(404).body(
                            Map.of("success", false, "message", "Invitation not found"));
                }

                if (!"pending".equals(invitation.getStatus())) {
                    return ResponseEntity.status(400).body(
                            Map.of("success", false, "message", "Invitation is no longer pending"));
                }

                // Check if the invited email has an existing account
                User existingUser = userRepository.findByEmail(invitation.getInvitedEmail()).orElse(null);
                Long acceptedByUserId = existingUser != null ? existingUser.getId() : null;

                // Get the mindmap and add collaborator
                Mindmap mindmap = mindmapRepository.findById(invitation.getMindmapId()).orElse(null);
                if (mindmap != null) {
                    // Add collaborator to mindmap
                    Collaborator collaborator = Collaborator.builder()
                            .mysqlUserId(acceptedByUserId)
                            .email(invitation.getInvitedEmail())
                            .role(invitation.getRole().toString())
                            .invitedBy(invitation.getInvitedByUserId())
                            .invitedAt(invitation.getCreatedAt())
                            .status("accepted")
                            .acceptedAt(java.time.LocalDateTime.now())
                            .build();

                    // Check if collaborator already exists
                    boolean collaboratorExists = mindmap.getCollaborators().stream()
                            .anyMatch(c -> invitation.getInvitedEmail().equals(c.getEmail()));

                    if (!collaboratorExists) {
                        mindmap.getCollaborators().add(collaborator);
                        mindmapRepository.save(mindmap);
                    }
                }

                // Mark invitation as accepted
                invitation.setStatus("accepted");
                invitation.setAcceptedAt(java.time.LocalDateTime.now());
                invitation.setAcceptedByUserId(acceptedByUserId);
                invitationRepository.save(invitation);

                // Notify the inviter
                collaborationService.notifyInviterOfAcceptance(invitation, acceptedByUserId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);

                if (existingUser != null) {
                    // User has an account - they can sign in and access immediately
                    response.put("message", "Invitation accepted! Please sign in to access the mindmap.");
                } else {
                    // User doesn't have an account - they need to register
                    response.put("message",
                            "Invitation accepted! Please create an account with this email to access the mindmap.");
                }
                response.put("mindmapId", mindmap != null ? mindmap.getId() : null);
                response.put("mindmapTitle", mindmap != null ? mindmap.getTitle() : null);
                response.put("requiresAuth", true);

                return ResponseEntity.ok(response);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(
                    Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error accepting invitation", e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "Error accepting invitation"));
        }
    }

    /**
     * Decline invitation via email link (public, no auth required)
     * POST /api/invitations/{token}/decline
     */
    @PostMapping("/{token}/decline")
    public ResponseEntity<?> declineInvitationPublic(
            @PathVariable String token,
            Authentication authentication) {

        try {
            Long userId = null;
            if (authentication != null) {
                try {
                    String email = authentication.getName();
                    User user = userDetailsService.loadUserEntityByEmail(email);
                    userId = user.getId();
                } catch (Exception e) {
                    // User not authenticated, will handle below
                }
            }

            if (userId != null) {
                // Authenticated user - use standard flow
                collaborationService.rejectInvitation(token, userId);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Invitation declined successfully"));
            } else {
                // Non-authenticated user - decline via token only
                CollaborationInvitation invitation = invitationRepository.findByToken(token)
                        .orElse(null);

                if (invitation == null) {
                    return ResponseEntity.status(404).body(
                            Map.of("success", false, "message", "Invitation not found"));
                }

                if (!"pending".equals(invitation.getStatus())) {
                    return ResponseEntity.status(400).body(
                            Map.of("success", false, "message", "Invitation is no longer pending"));
                }

                // Mark as rejected
                invitation.setStatus("rejected");
                invitation.setRejectedAt(java.time.LocalDateTime.now());
                invitationRepository.save(invitation);

                // Notify the inviter
                collaborationService.notifyInviterOfRejection(invitation, null);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Invitation declined successfully"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(
                    Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error declining invitation", e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "Error declining invitation"));
        }
    }
}
