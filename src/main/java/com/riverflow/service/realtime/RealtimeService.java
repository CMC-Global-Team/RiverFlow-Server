package com.riverflow.service.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to emit events to the Realtime Server via HTTP.
 * Used for broadcasting access control events (e.g., access revoked,
 * collaborator removed).
 */
@Service
@Slf4j
public class RealtimeService {

    private final RestTemplate restTemplate;
    private final String realtimeServerUrl;

    public RealtimeService(
            RestTemplate restTemplate,
            @Value("${realtime.server.url}") String realtimeServerUrl) {
        this.restTemplate = restTemplate;
        this.realtimeServerUrl = realtimeServerUrl;
    }

    /**
     * Emit a socket event to a specific mindmap room.
     *
     * @param mindmapId The mindmap ID (used to construct the room name)
     * @param event     The event name to emit (e.g., "mindmap:access:revoked")
     * @param data      The data payload to send with the event
     */
    @Async
    public void emitToRoom(String mindmapId, String event, Map<String, Object> data) {
        try {
            String url = realtimeServerUrl + "/realtime/mindmap/event";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("mindmapId", mindmapId);
            requestBody.put("event", event);
            requestBody.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            restTemplate.postForEntity(url, request, Map.class);

            log.info("Emitted event '{}' to room mindmap:{} with data: {}", event, mindmapId, data);
        } catch (Exception e) {
            log.error("Failed to emit event '{}' to room mindmap:{}: {}", event, mindmapId, e.getMessage());
        }
    }

    /**
     * Emit access revoked event when public access is disabled or set to private.
     *
     * @param mindmapId The mindmap ID
     * @param reason    The reason for access revocation
     */
    public void emitAccessRevoked(String mindmapId, String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("mindmapId", mindmapId);
        data.put("reason", reason);
        emitToRoom(mindmapId, "mindmap:access:revoked", data);
    }

    /**
     * Emit collaborator removed event when a collaborator is removed from mindmap.
     *
     * @param mindmapId     The mindmap ID
     * @param removedUserId The MySQL user ID of the removed collaborator
     * @param removedEmail  The email of the removed collaborator
     */
    public void emitCollaboratorRemoved(String mindmapId, Long removedUserId, String removedEmail) {
        Map<String, Object> data = new HashMap<>();
        data.put("mindmapId", mindmapId);
        data.put("removedUserId", removedUserId);
        data.put("removedEmail", removedEmail);
        emitToRoom(mindmapId, "mindmap:collaborator:removed", data);
    }

    /**
     * Emit public access level changed event when owner changes view/edit
     * permission.
     *
     * @param mindmapId      The mindmap ID
     * @param oldAccessLevel The previous access level (view/edit)
     * @param newAccessLevel The new access level (view/edit)
     */
    public void emitPublicAccessChanged(String mindmapId, String oldAccessLevel, String newAccessLevel) {
        Map<String, Object> data = new HashMap<>();
        data.put("mindmapId", mindmapId);
        data.put("oldAccessLevel", oldAccessLevel);
        data.put("newAccessLevel", newAccessLevel);
        emitToRoom(mindmapId, "mindmap:public:permission:changed", data);
    }

    /**
     * Emit collaborator role changed event when owner changes a collaborator's
     * role.
     *
     * @param mindmapId The mindmap ID
     * @param userId    The MySQL user ID of the collaborator
     * @param email     The email of the collaborator
     * @param oldRole   The previous role (EDITOR/VIEWER)
     * @param newRole   The new role (EDITOR/VIEWER)
     */
    public void emitCollaboratorRoleChanged(String mindmapId, Long userId, String email, String oldRole,
            String newRole) {
        Map<String, Object> data = new HashMap<>();
        data.put("mindmapId", mindmapId);
        data.put("userId", userId);
        data.put("email", email);
        data.put("oldRole", oldRole);
        data.put("newRole", newRole);
        emitToRoom(mindmapId, "mindmap:collaborator:role:changed", data);
    }
}
