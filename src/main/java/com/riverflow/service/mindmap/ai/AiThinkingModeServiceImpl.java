package com.riverflow.service.mindmap.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.UpdateMindmapRequest;
import com.riverflow.dto.mindmap.ai.Action;
import com.riverflow.dto.mindmap.ai.ActionList;
import com.riverflow.dto.mindmap.ai.Otmz;
import com.riverflow.exception.mindmap.MindmapNotFoundException;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.service.mindmap.MindmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiThinkingModeServiceImpl implements AiThinkingModeService {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;
    private final GeminiPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;
    private final ObjectMapper objectMapper;
    private final AiOperationExecutor operationExecutor;
    private final LayoutEngine layoutEngine;
    private final MindmapRepository mindmapRepository;
    private final MindmapService mindmapService;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${realtime.server.url:}")
    private String realtimeServerUrl;

    @Override
    public Otmz think(String topic, String language, String structureType, Integer levels, Integer firstLevelCount, List<String> tags, String mode, String mindmapId) {
        Map<String, Object> payload = promptBuilder.buildThinkingOtmzPrompt(
                topic, language, structureType, levels, firstLevelCount, tags, mode
        );
        String text = callGeminiStream(payload, mindmapId);
        String json = promptBuilder.ensureJson(text);
        return responseParser.parseOtmz(json);
    }

    @Override
    public ActionList plan(Otmz otmz, String language, String mindmapId) {
        try {
            String otmzJson = objectMapper.writeValueAsString(otmz);
            Map<String, Object> payload = promptBuilder.buildActionListPrompt(otmzJson, language);
            String text = callGeminiStream(payload, mindmapId);
            String json = promptBuilder.ensureJson(text);
            return responseParser.parseActionList(json);
        } catch (Exception e) {
            return new ActionList();
        }
    }

    @Override
    public MindmapResponse generate(ActionList actionList, String mindmapId, String structureType, Long userId) {
        try {
            // 1. Load mindmap and validate permissions
            Mindmap mindmap = mindmapRepository.findById(mindmapId)
                    .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

            validatePermissions(mindmap, userId);

            // 2. Convert ActionList to operations format
            List<Map<String, Object>> operations = new ArrayList<>();
            if (actionList != null && actionList.getActions() != null) {
                for (Action action : actionList.getActions()) {
                    Map<String, Object> op = new HashMap<>();
                    op.put("type", action.getType());
                    if (action.getParams() != null) {
                        op.putAll(action.getParams());
                    }
                    operations.add(op);
                }
            }

            // 3. Execute operations to create nodes
            List<String> logs = operationExecutor.executeOperations(operations, mindmap);

            // 4. Apply layout
            String struct = structureType != null ? structureType : "mindmap";
            layoutEngine.applyLayout(struct, mindmap.getNodes(), mindmap.getEdges());

            // 5. Save mindmap
            UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                    .nodes(mindmap.getNodes())
                    .edges(mindmap.getEdges())
                    .build();

            return mindmapService.updateMindmap(mindmapId, updateReq, userId);
        } catch (Exception e) {
            e.printStackTrace(); // Log stack trace to console/docker logs
            throw e; // Rethrow to let global handler return 500
        }
    }

    /**
     * Validate user has permission to modify the mindmap
     */
    private void validatePermissions(Mindmap mindmap, Long userId) {
        boolean isOwner = mindmap.getMysqlUserId() != null && mindmap.getMysqlUserId().equals(userId);

        boolean isEditorCollaborator = mindmap.getCollaborators() != null &&
                mindmap.getCollaborators().stream()
                        .anyMatch(c ->
                                c.getMysqlUserId() != null &&
                                        c.getMysqlUserId().equals(userId) &&
                                        "accepted".equals(c.getStatus()) &&
                                        "EDITOR".equals(c.getRole())
                        );

        if (!isOwner && !isEditorCollaborator) {
            throw new com.riverflow.exception.mindmap.MindmapAccessDeniedException(mindmap.getId(), userId);
        }
    }

    /**
     * Send realtime event to WebSocket server
     */
    private void sendRealtimeEvent(String mindmapId, String event, Map<String, Object> data) {
        if (realtimeServerUrl == null || realtimeServerUrl.isBlank()) {
            return;
        }
        try {
            String room = "mindmap:" + mindmapId;
            Map<String, Object> payload = new HashMap<>();
            payload.put("room", room);
            payload.put("event", event);
            payload.put("data", data != null ? data : Map.of());

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(realtimeServerUrl + "/realtime/mindmap/event"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(resp -> {
                        if (resp.statusCode() >= 400) {
                        }
                    })
                    .exceptionally(ex -> {
                        return null;
                    });
        } catch (Exception e) {
        }
    }

    /**
     * Extract natural language part (before JSON block)
     */
    private String extractNaturalLanguage(String text) {
        if (text == null)
            return "";

        // Find the start of JSON block
        int jsonStart = text.indexOf("```json");
        if (jsonStart < 0) {
            jsonStart = text.indexOf('{');
        }

        if (jsonStart > 0) {
            // Return everything before the JSON
            return text.substring(0, jsonStart).trim();
        }

        // If no JSON found, return the whole text
        return text.trim();
    }

    /**
     * Call Gemini with streaming support
     */
    private String callGeminiStream(Map<String, Object> payload, String mindmapId) {
        try {
            String url = "/v1beta/models/" + model + ":streamGenerateContent";
            StringBuilder fullText = new StringBuilder();
            StringBuilder naturalLanguagePart = new StringBuilder();
            final boolean[] jsonStarted = { false }; // Use array to make it effectively final

            // Send streaming start event
            if (mindmapId != null) {
                sendRealtimeEvent(mindmapId, "ai:stream:start", Map.of());
            }

            Flux<Map> responseFlux = geminiWebClient.post()
                    .uri(url)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToFlux(Map.class);

            Iterable<Map> iterable = responseFlux.toIterable();
            iterable.forEach(chunk -> {
                try {
                    List<?> candidates = (List<?>) chunk.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                        Map<?, ?> content = (Map<?, ?>) candidate.get("content");
                        if (content != null) {
                            List<?> parts = (List<?>) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                Map<?, ?> part = (Map<?, ?>) parts.get(0);
                                String text = String.valueOf(part.get("text"));
                                if (text != null && !"null".equals(text)) {
                                    fullText.append(text);

                                    // Only send natural language part to client (before JSON)
                                    if (!jsonStarted[0]) {
                                        // Check if this chunk starts JSON
                                        if (text.contains("```json") || text.contains("{")) {
                                            jsonStarted[0] = true;
                                            // Send only the part before JSON marker
                                            String beforeJson = extractNaturalLanguage(text);
                                            if (!beforeJson.isEmpty()) {
                                                naturalLanguagePart.append(beforeJson);
                                                if (mindmapId != null) {
                                                    sendRealtimeEvent(mindmapId, "ai:stream:chunk",
                                                            Map.of("chunk", beforeJson, "done", false));
                                                }
                                            }
                                        } else {
                                            // This is pure natural language, send it
                                            naturalLanguagePart.append(text);
                                            if (mindmapId != null) {
                                                sendRealtimeEvent(mindmapId, "ai:stream:chunk",
                                                        Map.of("chunk", text, "done", false));
                                            }
                                        }
                                    }
                                    // If JSON already started, don't send those chunks
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
            });

            // Send streaming done event with natural language summary
            if (mindmapId != null) {
                String naturalLangFinal = naturalLanguagePart.toString().trim();
                if (naturalLangFinal.isEmpty()) {
                    naturalLangFinal = extractNaturalLanguage(fullText.toString());
                }
                sendRealtimeEvent(mindmapId, "ai:stream:done",
                        Map.of("fullText", naturalLangFinal));
            }

            return fullText.toString();
        } catch (Exception e) {
            if (mindmapId != null) {
                sendRealtimeEvent(mindmapId, "ai:stream:error",
                        Map.of("error", e.getMessage()));
            }
            throw new RuntimeException("AI service failed: " + e.getMessage());
        }
    }

    // Basic non-streaming Gemini call (kept for backward compatibility if needed)
    private String callGemini(Map<String, Object> payload) {
        try {
            String url = "/v1beta/models/" + model + ":generateContent";
            Map<?, ?> response = geminiWebClient.post()
                    .uri(url)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Gemini API returned null");
            }

            Object candidatesObj = response.get("candidates");
            if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response");
            }

            Object contentObj = ((Map<?, ?>) candidates.get(0)).get("content");
            if (!(contentObj instanceof Map<?, ?> content)) {
                throw new RuntimeException("Invalid Gemini response: missing content");
            }
            Object partsObj = content.get("parts");
            if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
                throw new RuntimeException("Invalid Gemini response: missing parts");
            }
            Object text = ((Map<?, ?>) parts.get(0)).get("text");
            return text == null ? null : String.valueOf(text);
        } catch (Exception e) {
            throw new RuntimeException("AI service failed: " + e.getMessage());
        }
    }
}
