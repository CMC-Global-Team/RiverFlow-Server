package com.riverflow.service.mindmap;

import com.riverflow.dto.mindmap.CreateMindmapRequest;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.MindmapSummaryResponse;
import com.riverflow.dto.mindmap.UpdateMindmapRequest;
import com.riverflow.exception.mindmap.MindmapAccessDeniedException;
import com.riverflow.exception.mindmap.MindmapNotFoundException;
import com.riverflow.model.User;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.repository.UserRepository;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.util.mindmap.MindmapMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.riverflow.service.mindmap.MindmapHistoryService;
import com.riverflow.service.mindmap.UndoRedoService;
import com.riverflow.service.realtime.RealtimeService;
import com.riverflow.model.mindmap.subdocuments.Collaborator;
import com.riverflow.model.mindmap.subdocuments.MindmapMetadata;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Mindmap operations
 */
@Service
@RequiredArgsConstructor
public class MindmapServiceImpl implements MindmapService {

    private final MindmapRepository mindmapRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final MindmapHistoryService historyService;
    private final UndoRedoService undoRedoService;
    private final RealtimeService realtimeService;

    @Override
    @Transactional
    public MindmapResponse createMindmap(CreateMindmapRequest request, Long userId) {
        Mindmap mindmap = Mindmap.builder()
                .mysqlUserId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnail(request.getThumbnail())
                .nodes(request.getNodes() != null ? request.getNodes() : new ArrayList<>())
                .edges(request.getEdges() != null ? request.getEdges() : new ArrayList<>())
                .viewport(MindmapMapper.toViewportEntity(request.getViewport()))
                .settings(MindmapMapper.toSettingsEntity(request.getSettings()))
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .isFavorite(request.getIsFavorite() != null ? request.getIsFavorite() : false)
                .isTemplate(request.getIsTemplate() != null ? request.getIsTemplate() : false)
                .tags(request.getTags())
                .category(request.getCategory())
                .aiGenerated(request.getAiGenerated() != null ? request.getAiGenerated() : false)
                .aiWorkflowId(request.getAiWorkflowId())
                .aiMetadata(request.getAiMetadata())
                .status("active")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Mindmap savedMindmap = mindmapRepository.save(mindmap);
        historyService.recordChange(
                savedMindmap.getId(),
                userId,
                "create_mindmap",
                null,
                MindmapMapper.toResponse(savedMindmap));

        MindmapResponse response = MindmapMapper.toResponse(savedMindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(savedMindmap.getId(), userId));
        response.setCanRedo(undoRedoService.checkCanRedo(savedMindmap.getId(), userId));

        return response;
    }

    @Override
    public MindmapResponse getMindmapById(String mindmapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Check if user has access
        if (!hasAccess(mindmap, userId)) {
            // If mindmap is public, include shareToken in exception so client can load via
            // public API
            String shareToken = null;
            if (Boolean.TRUE.equals(mindmap.getIsPublic()) && mindmap.getShareToken() != null) {
                shareToken = mindmap.getShareToken();
            }
            throw new MindmapAccessDeniedException(mindmapId, userId, shareToken);
        }

        MindmapResponse response = MindmapMapper.toResponse(mindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(mindmapId, userId));
        response.setCanRedo(undoRedoService.checkCanRedo(mindmapId, userId));

        return response;
    }

    @Override
    @Transactional
    public MindmapResponse updateMindmap(String mindmapId, UpdateMindmapRequest request, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Check if user is the owner
        boolean isOwner = mindmap.getMysqlUserId().equals(userId);

        // Check if user is an EDITOR collaborator
        boolean isEditorCollaborator = mindmap.getCollaborators() != null &&
                mindmap.getCollaborators().stream()
                        .anyMatch(c -> c.getMysqlUserId() != null &&
                                c.getMysqlUserId().equals(userId) &&
                                "accepted".equals(c.getStatus()) &&
                                "EDITOR".equals(c.getRole()));

        if (!isOwner && !isEditorCollaborator) {
            throw new MindmapAccessDeniedException(mindmapId, userId);
        }

        MindmapResponse oldMindmapState = MindmapMapper.toResponse(mindmap);

        // Update fields if provided
        if (request.getTitle() != null) {
            mindmap.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            mindmap.setDescription(request.getDescription());
        }
        if (request.getThumbnail() != null) {
            mindmap.setThumbnail(request.getThumbnail());
        }
        if (request.getNodes() != null) {
            mindmap.setNodes(request.getNodes());
        }
        if (request.getEdges() != null) {
            mindmap.setEdges(request.getEdges());
        }
        if (request.getViewport() != null) {
            mindmap.setViewport(MindmapMapper.toViewportEntity(request.getViewport()));
        }
        if (request.getSettings() != null) {
            mindmap.setSettings(MindmapMapper.toSettingsEntity(request.getSettings()));
        }
        if (request.getTags() != null) {
            mindmap.setTags(request.getTags());
        }
        if (request.getCategory() != null) {
            mindmap.setCategory(request.getCategory());
        }
        if (request.getIsPublic() != null) {
            mindmap.setIsPublic(request.getIsPublic());
        }
        if (request.getIsFavorite() != null) {
            mindmap.setIsFavorite(request.getIsFavorite());
        }
        if (request.getIsTemplate() != null) {
            mindmap.setIsTemplate(request.getIsTemplate());
        }
        if (request.getStatus() != null) {
            mindmap.setStatus(request.getStatus());
        }
        if (request.getAiMetadata() != null) {
            mindmap.setAiMetadata(request.getAiMetadata());
        }

        mindmap.setUpdatedAt(LocalDateTime.now());

        Mindmap updatedMindmap = mindmapRepository.save(mindmap);
        historyService.recordChange(
                mindmapId,
                userId,
                "update_mindmap",
                oldMindmapState, // Trạng thái 'before'
                MindmapMapper.toResponse(updatedMindmap) // Trạng thái 'after'
        );

        MindmapResponse response = MindmapMapper.toResponse(updatedMindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(mindmapId, userId));
        response.setCanRedo(undoRedoService.checkCanRedo(mindmapId, userId));

        return response;
    }

    @Override
    @Transactional
    public void deleteMindmap(String mindmapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Check if user is the owner
        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException(mindmapId, userId);
        }

        // Emit deleted event BEFORE deleting to notify all connected users
        realtimeService.emitMindmapDeleted(mindmapId);

        // Hard delete - permanently remove from database
        mindmapRepository.delete(mindmap);
    }

    @Override
    @Transactional
    public void permanentlyDeleteMindmap(String mindmapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Check if user is the owner
        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException(mindmapId, userId);
        }

        mindmapRepository.delete(mindmap);
    }

    @Override
    public List<MindmapSummaryResponse> getAllMindmapsByUser(Long userId) {
        // Get mindmaps owned by user
        List<Mindmap> ownedMindmaps = mindmapRepository
                .findByMysqlUserIdAndStatusOrderByUpdatedAtDesc(userId, "active");

        // Get mindmaps where user is an accepted collaborator
        Query collaboratorQuery = new Query();
        collaboratorQuery.addCriteria(
                Criteria.where("collaborators").elemMatch(
                        Criteria.where("mysqlUserId").is(userId)
                                .and("status").is("accepted"))
                        .and("status").is("active"));
        List<Mindmap> collaboratedMindmaps = mongoTemplate.find(collaboratorQuery, Mindmap.class);

        // Combine results and remove duplicates
        List<Mindmap> allMindmaps = new ArrayList<>(ownedMindmaps);
        collaboratedMindmaps.forEach(m -> {
            if (allMindmaps.stream().noneMatch(om -> om.getId().equals(m.getId()))) {
                allMindmaps.add(m);
            }
        });

        // Sort by updatedAt descending
        allMindmaps.sort((m1, m2) -> m2.getUpdatedAt().compareTo(m1.getUpdatedAt()));

        return allMindmaps.stream()
                .map(mindmap -> {
                    // Get owner info
                    User owner = userRepository.findById(mindmap.getMysqlUserId()).orElse(null);
                    String ownerName = owner != null ? owner.getFullName() : "Unknown";
                    String ownerAvatar = owner != null ? owner.getAvatar() : null;
                    return MindmapMapper.toSummaryResponse(mindmap, ownerName, ownerAvatar);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MindmapSummaryResponse> getMindmapsByCategory(Long userId, String category) {
        List<Mindmap> mindmaps = mindmapRepository
                .findByMysqlUserIdAndCategoryAndStatus(userId, category, "active");

        return mindmaps.stream()
                .map(mindmap -> {
                    // Get owner info
                    User owner = userRepository.findById(mindmap.getMysqlUserId()).orElse(null);
                    String ownerName = owner != null ? owner.getFullName() : "Unknown";
                    String ownerAvatar = owner != null ? owner.getAvatar() : null;
                    return MindmapMapper.toSummaryResponse(mindmap, ownerName, ownerAvatar);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MindmapSummaryResponse> getFavoriteMindmaps(Long userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("mysqlUserId").is(userId)
                .and("isFavorite").is(true)
                .and("status").is("active"));

        List<Mindmap> mindmaps = mongoTemplate.find(query, Mindmap.class);

        return mindmaps.stream()
                .map(mindmap -> {
                    // Get owner info
                    User owner = userRepository.findById(mindmap.getMysqlUserId()).orElse(null);
                    String ownerName = owner != null ? owner.getFullName() : "Unknown";
                    String ownerAvatar = owner != null ? owner.getAvatar() : null;
                    return MindmapMapper.toSummaryResponse(mindmap, ownerName, ownerAvatar);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MindmapSummaryResponse> getArchivedMindmaps(Long userId) {
        List<Mindmap> mindmaps = mindmapRepository
                .findByMysqlUserIdAndStatus(userId, "archived");

        return mindmaps.stream()
                .map(mindmap -> {
                    // Get owner info
                    User owner = userRepository.findById(mindmap.getMysqlUserId()).orElse(null);
                    String ownerName = owner != null ? owner.getFullName() : "Unknown";
                    String ownerAvatar = owner != null ? owner.getAvatar() : null;
                    return MindmapMapper.toSummaryResponse(mindmap, ownerName, ownerAvatar);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MindmapResponse toggleFavorite(String mindmapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Check if user is the owner
        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException(mindmapId, userId);
        }

        Boolean oldState = mindmap.getIsFavorite();
        Boolean newState = !oldState;

        mindmap.setIsFavorite(newState);
        mindmap.setUpdatedAt(LocalDateTime.now());
        Mindmap updatedMindmap = mindmapRepository.save(mindmap);

        historyService.recordChange(mindmapId, userId, "toggle_favorite", oldState, newState);
        MindmapResponse response = MindmapMapper.toResponse(updatedMindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(mindmapId, userId));
        response.setCanRedo(undoRedoService.checkCanRedo(mindmapId, userId));

        return response;
    }

    @Override
    @Transactional
    public MindmapResponse archiveMindmap(String mindmapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Check if user is the owner
        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException(mindmapId, userId);
        }

        String oldState = mindmap.getStatus();

        mindmap.setStatus("archived");
        mindmap.setUpdatedAt(LocalDateTime.now());
        Mindmap updatedMindmap = mindmapRepository.save(mindmap);

        historyService.recordChange(mindmapId, userId, "archive_mindmap", oldState, "archived");
        MindmapResponse response = MindmapMapper.toResponse(updatedMindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(mindmapId, userId));
        response.setCanRedo(undoRedoService.checkCanRedo(mindmapId, userId));

        return response;
    }

    @Override
    @Transactional
    public MindmapResponse unarchiveMindmap(String mindmapId, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Check if user is the owner
        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException(mindmapId, userId);
        }

        String oldState = mindmap.getStatus();

        mindmap.setStatus("active");
        mindmap.setUpdatedAt(LocalDateTime.now());
        Mindmap updatedMindmap = mindmapRepository.save(mindmap);

        historyService.recordChange(mindmapId, userId, "unarchive_mindmap", oldState, "active");
        MindmapResponse response = MindmapMapper.toResponse(updatedMindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(mindmapId, userId));
        response.setCanRedo(undoRedoService.checkCanRedo(mindmapId, userId));

        return response;
    }

    @Override
    public List<MindmapSummaryResponse> searchMindmaps(Long userId, String keyword) {
        Query query = new Query();

        Criteria criteria = new Criteria().andOperator(
                Criteria.where("mysqlUserId").is(userId),
                Criteria.where("status").is("active"),
                new Criteria().orOperator(
                        Criteria.where("title").regex(keyword, "i"),
                        Criteria.where("description").regex(keyword, "i")));

        query.addCriteria(criteria);

        List<Mindmap> mindmaps = mongoTemplate.find(query, Mindmap.class);

        return mindmaps.stream()
                .map(mindmap -> {
                    // Get owner info
                    User owner = userRepository.findById(mindmap.getMysqlUserId()).orElse(null);
                    String ownerName = owner != null ? owner.getFullName() : "Unknown";
                    String ownerAvatar = owner != null ? owner.getAvatar() : null;
                    return MindmapMapper.toSummaryResponse(mindmap, ownerName, ownerAvatar);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MindmapResponse duplicateMindmap(String originalMapId, Long userId) {
        Mindmap originalMindmap = mindmapRepository.findById(originalMapId)
                .orElseThrow(() -> new MindmapNotFoundException(originalMapId, userId));

        if (!hasAccess(originalMindmap, userId)) {
            throw new MindmapAccessDeniedException(originalMapId, userId);
        }

        Collaborator newOwner = Collaborator.builder()
                .mysqlUserId(userId)
                .role("owner")
                .status("accepted")
                .invitedBy(userId)
                .acceptedAt(LocalDateTime.now())
                .build();

        MindmapMetadata newMetadata = MindmapMetadata.builder()
                .nodeCount(originalMindmap.getNodes().size()) // Copy số node
                .edgeCount(originalMindmap.getEdges().size()) // Copy số edge
                .forkedFrom(originalMapId)
                .viewCount(0)
                .build();

        Mindmap newMindmap = Mindmap.builder()
                .id(null)
                .mysqlUserId(userId)
                .title("Copy of " + (originalMindmap.getTitle() != null ? originalMindmap.getTitle() : "Untitled"))
                .status("active")
                .isFavorite(false)
                .shareToken(null)
                .collaborators(List.of(newOwner))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .metadata(newMetadata)
                .description(originalMindmap.getDescription())
                .thumbnail(originalMindmap.getThumbnail())
                .viewport(originalMindmap.getViewport())
                .settings(originalMindmap.getSettings())
                .category(originalMindmap.getCategory())
                .aiGenerated(originalMindmap.getAiGenerated())
                .aiWorkflowId(originalMindmap.getAiWorkflowId())
                .aiMetadata(originalMindmap.getAiMetadata())
                .build();

        newMindmap.setNodes(
                originalMindmap.getNodes() != null ? new ArrayList<>(originalMindmap.getNodes()) : new ArrayList<>());
        newMindmap.setEdges(
                originalMindmap.getEdges() != null ? new ArrayList<>(originalMindmap.getEdges()) : new ArrayList<>());
        newMindmap.setTags(
                originalMindmap.getTags() != null ? new ArrayList<>(originalMindmap.getTags()) : new ArrayList<>());

        Mindmap savedMindmap = mindmapRepository.save(newMindmap);
        historyService.recordChange(
                savedMindmap.getId(),
                userId,
                "create_duplicate",
                originalMapId,
                MindmapMapper.toResponse(savedMindmap));

        MindmapResponse response = MindmapMapper.toResponse(savedMindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(savedMindmap.getId(), userId));
        response.setCanRedo(undoRedoService.checkCanRedo(savedMindmap.getId(), userId));

        return response;
    }

    /**
     * Update public access level
     */
    @Override
    @Transactional
    public MindmapResponse updatePublicAccess(String mindmapId, Boolean isPublic, String accessLevel, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException("Chỉ chủ sở hữu mới có quyền cập nhật.", mindmapId, userId);
        }

        // Track previous state to detect access changes
        Boolean wasPublic = mindmap.getIsPublic();
        String oldAccessLevel = mindmap.getPublicAccessLevel();

        mindmap.setIsPublic(isPublic);
        mindmap.setPublicAccessLevel(accessLevel != null ? accessLevel : "private");
        mindmap.setUpdatedAt(LocalDateTime.now());

        // Generate shareToken if making public and it doesn't have one
        if (Boolean.TRUE.equals(isPublic) && mindmap.getShareToken() == null) {
            mindmap.setShareToken(java.util.UUID.randomUUID().toString());
        }

        Mindmap updatedMindmap = mindmapRepository.save(mindmap);

        // Emit access revoked event if public access was disabled or changed to private
        boolean accessRevoked = Boolean.TRUE.equals(wasPublic) &&
                (!Boolean.TRUE.equals(isPublic) || "private".equalsIgnoreCase(accessLevel));
        if (accessRevoked) {
            realtimeService.emitAccessRevoked(mindmapId, "public_access_disabled");
        } else if (Boolean.TRUE.equals(wasPublic) && Boolean.TRUE.equals(isPublic)) {
            // Emit permission changed if access level changed between view/edit
            String normalizedOld = oldAccessLevel != null ? oldAccessLevel.toLowerCase() : "";
            String normalizedNew = accessLevel != null ? accessLevel.toLowerCase() : "";
            if (!normalizedOld.equals(normalizedNew) &&
                    (("view".equals(normalizedOld) && "edit".equals(normalizedNew)) ||
                            ("edit".equals(normalizedOld) && "view".equals(normalizedNew)))) {
                realtimeService.emitPublicAccessChanged(mindmapId, oldAccessLevel, accessLevel);
            }
        }

        MindmapResponse response = MindmapMapper.toResponse(updatedMindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(mindmapId, userId));
        response.setCanRedo(undoRedoService.checkCanRedo(mindmapId, userId));

        return response;
    }

    /**
     * Check if user has access to mindmap
     */
    private boolean hasAccess(Mindmap mindmap, Long userId) {
        // Public mindmaps are accessible to everyone (including unauthenticated users)
        if (Boolean.TRUE.equals(mindmap.getIsPublic())) {
            return true;
        }

        // If user is not authenticated, only public mindmaps are accessible
        if (userId == null) {
            return false;
        }

        // Owner has access
        if (mindmap.getMysqlUserId() != null && mindmap.getMysqlUserId().equals(userId)) {
            return true;
        }

        // Check if user is a collaborator (accepted or pending status)
        if (mindmap.getCollaborators() != null && !mindmap.getCollaborators().isEmpty()) {
            boolean hasAccess = mindmap.getCollaborators().stream()
                    .anyMatch(collaborator -> collaborator.getMysqlUserId() != null &&
                            collaborator.getMysqlUserId().equals(userId) &&
                            ("accepted".equals(collaborator.getStatus())
                                    || "pending".equals(collaborator.getStatus())));
            if (hasAccess) {
                return true;
            }

            // Log all collaborators for debugging
            mindmap.getCollaborators().forEach(c -> {
            });
        } else {
        }

        return false;
    }

    @Override
    @Transactional
    public MindmapResponse updateMindmapByShareToken(String shareToken, UpdateMindmapRequest request) {
        Mindmap mindmap = mindmapRepository.findByShareToken(shareToken)
                .orElseThrow(
                        () -> new MindmapNotFoundException("Mindmap not found with shareToken: " + shareToken, null));

        if (!Boolean.TRUE.equals(mindmap.getIsPublic()) || !"edit".equals(mindmap.getPublicAccessLevel())) {
            throw new MindmapAccessDeniedException("This mindmap is not editable publicly", mindmap.getId(), null);
        }

        MindmapResponse oldState = MindmapMapper.toResponse(mindmap);

        if (request.getTitle() != null)
            mindmap.setTitle(request.getTitle());
        if (request.getNodes() != null)
            mindmap.setNodes(request.getNodes());
        if (request.getEdges() != null)
            mindmap.setEdges(request.getEdges());
        if (request.getViewport() != null)
            mindmap.setViewport(MindmapMapper.toViewportEntity(request.getViewport()));
        mindmap.setUpdatedAt(LocalDateTime.now());

        Mindmap updated = mindmapRepository.save(mindmap);

        historyService.recordChange(
                updated.getId(),
                mindmap.getMysqlUserId(),
                "public_update",
                oldState,
                MindmapMapper.toResponse(updated));

        return MindmapMapper.toResponse(updated);
    }

    @Override
    public MindmapResponse getMindmapByShareToken(String shareToken) {
        Mindmap mindmap = mindmapRepository.findByShareToken(shareToken)
                .orElseThrow(
                        () -> new MindmapNotFoundException("Mindmap not found with shareToken: " + shareToken, null));

        // Verify mindmap is public
        if (!Boolean.TRUE.equals(mindmap.getIsPublic())) {
            throw new MindmapNotFoundException("This mindmap is not public", null);
        }

        MindmapResponse response = MindmapMapper.toResponse(mindmap);

        // Add owner info
        User owner = userRepository.findById(mindmap.getMysqlUserId()).orElse(null);
        if (owner != null) {
            response.setOwnerName(owner.getFullName());
            response.setOwnerAvatar(owner.getAvatar());
        }

        response.setCanUndo(false);
        response.setCanRedo(false);

        return response;
    }

    @Override
    @Transactional
    public MindmapResponse updateEmbedSettings(String mindmapId, Boolean isEmbedEnabled, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Only owner can update embed settings
        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException("Only owner can update embed settings", mindmapId, userId);
        }

        mindmap.setIsEmbedEnabled(isEmbedEnabled);

        // Generate embed token if enabling and no token exists
        if (Boolean.TRUE.equals(isEmbedEnabled) && mindmap.getEmbedToken() == null) {
            mindmap.setEmbedToken(java.util.UUID.randomUUID().toString());
        }

        mindmap.setUpdatedAt(LocalDateTime.now());
        Mindmap updatedMindmap = mindmapRepository.save(mindmap);

        MindmapResponse response = MindmapMapper.toResponse(updatedMindmap);
        response.setCanUndo(undoRedoService.checkCanUndo(mindmapId, userId));
        response.setCanRedo(undoRedoService.checkCanRedo(mindmapId, userId));

        return response;
    }

    @Override
    public MindmapResponse getMindmapByEmbedToken(String embedToken) {
        Mindmap mindmap = mindmapRepository.findByEmbedToken(embedToken)
                .orElseThrow(() -> new MindmapNotFoundException("Embedded mindmap not found with token: " + embedToken,
                        null));

        // Verify embedding is enabled
        if (!Boolean.TRUE.equals(mindmap.getIsEmbedEnabled())) {
            throw new MindmapAccessDeniedException("Embedding is disabled for this mindmap", mindmap.getId(), null);
        }

        MindmapResponse response = MindmapMapper.toResponse(mindmap);

        // Add owner info
        User owner = userRepository.findById(mindmap.getMysqlUserId()).orElse(null);
        if (owner != null) {
            response.setOwnerName(owner.getFullName());
            response.setOwnerAvatar(owner.getAvatar());
        }

        // Embedded view is always read-only, no undo/redo
        response.setCanUndo(false);
        response.setCanRedo(false);

        return response;
    }
}
