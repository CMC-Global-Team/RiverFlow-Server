package com.riverflow.service.mindmap.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.CreateMindmapRequest;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.UpdateMindmapRequest;
import com.riverflow.dto.mindmap.ai.GenerateMindmapRequest;
import com.riverflow.dto.mindmap.ai.OptimizeRequest;
import com.riverflow.exception.mindmap.InvalidMindmapDataException;
import com.riverflow.exception.mindmap.MindmapAccessDeniedException;
import com.riverflow.exception.mindmap.MindmapNotFoundException;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.repository.UserRepository;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.service.mindmap.MindmapService;
import com.riverflow.service.mindmap.ai.AiResponseParser.AiDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Mindmap Service - 100% Gemini AI-driven
 * Zero hardcoded logic - all decisions made by AI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiMindmapServiceImpl implements AiMindmapService {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;
    private final MindmapRepository mindmapRepository;
    private final MindmapService mindmapService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // AI helpers - modular and clean
    private final AiOperationExecutor operationExecutor;
    private final AiResponseParser responseParser;
    private final GeminiPromptBuilder promptBuilder;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${realtime.server.url:}")
    private String realtimeServerUrl;

    @Override
    public MindmapResponse generateMindmap(GenerateMindmapRequest request, Long userId) {
        // Use Gemini to generate new mindmap from scratch
        String topic = request.getTopic().trim();
        String title = StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : topic;
        String lang = request.getLanguage() != null ? request.getLanguage() : "vi";
        int levels = request.getLevels() != null ? request.getLevels() : 2;
        String mode = determineMode(request.getMode());

        int minFirst = "normal".equalsIgnoreCase(mode) ? 3 : 4;
        int maxFirst = "normal".equalsIgnoreCase(mode) ? 5 : 6;
        int defaultFirst = "normal".equalsIgnoreCase(mode) ? 4 : 5;
        int firstLevelCount = request.getFirstLevelCount() != null
                ? Math.max(minFirst, Math.min(maxFirst, request.getFirstLevelCount()))
                : defaultFirst;

        // Deduct credits
        deductCredits(userId, mode);

        // Ask Gemini to generate mindmap
        Map<String, Object> payload = promptBuilder.buildGeneratePrompt(
                topic, levels, firstLevelCount, lang, request.getTags(), mode, minFirst, maxFirst);

        String json = callGemini(payload);
        JsonNode root = parseJson(promptBuilder.ensureJson(json));

        // Parse and validate nodes
        JsonNode nodesNode = root.get("nodes");
        if (nodesNode == null || !nodesNode.isArray()) {
            throw new InvalidMindmapDataException("nodes", "AI didn't return valid nodes array");
        }

        // Build ReactFlow structure
        List<Map<String, Object>> rfNodes = new ArrayList<>();
        List<Map<String, Object>> rfEdges = new ArrayList<>();
        Map<String, String> idMap = new HashMap<>();
        Map<String, String> parentByTempId = new HashMap<>();

        // First pass: collect parent relationships
        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));
            String parentTempId = textOrNull(n.get("parentId"));

            if (!StringUtils.hasText(tempId) || !StringUtils.hasText(label)) {
                continue; // skip invalid nodes
            }
            parentByTempId.put(tempId, parentTempId);
        }

        // Second pass: create nodes
        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));

            if (!StringUtils.hasText(tempId) || !StringUtils.hasText(label)) {
                continue;
            }

            String newId = "node-" + UUID.randomUUID();
            idMap.put(tempId, newId);

            Map<String, Object> rfNode = new HashMap<>();
            rfNode.put("id", newId);
            rfNode.put("type", "default");
            rfNode.put("data", Map.of("label", label));
            rfNode.put("position", Map.of("x", 0, "y", 0));
            rfNodes.add(rfNode);
        }

        // Third pass: create edges
        for (Map.Entry<String, String> entry : parentByTempId.entrySet()) {
            String childTemp = entry.getKey();
            String parentTemp = entry.getValue();

            if (!StringUtils.hasText(parentTemp) || !idMap.containsKey(parentTemp) || !idMap.containsKey(childTemp)) {
                continue;
            }

            Map<String, Object> edge = new HashMap<>();
            edge.put("id", "edge-" + UUID.randomUUID());
            edge.put("source", idMap.get(parentTemp));
            edge.put("target", idMap.get(childTemp));
            rfEdges.add(edge);
        }

        // Create mindmap
        CreateMindmapRequest createReq = CreateMindmapRequest.builder()
                .title(title)
                .nodes(rfNodes)
                .edges(rfEdges)
                .aiGenerated(true)
                .category("ai-generated")
                .build();

        return mindmapService.createMindmap(createReq, userId);
    }

    @Override
    public MindmapResponse optimize(OptimizeRequest request, Long userId) {
        // 100% AI-driven optimization - Gemini decides everything

        // 1. Load mindmap and validate permissions
        Mindmap mindmap = mindmapRepository.findById(request.getMindmapId())
                .orElseThrow(() -> new MindmapNotFoundException(request.getMindmapId(), userId));

        validatePermissions(mindmap, userId);

        // 2. Ask Gemini AI to analyze and plan operations
        String lang = request.getLanguage() != null ? request.getLanguage() : "vi";
        AiDecision decision = askAiForDecision(mindmap, request, lang);

        // 3. Execute AI-decided operations
        List<String> logs = new ArrayList<>();
        if (decision.hasOps()) {
            logs.addAll(operationExecutor.executeOperations(decision.ops(), mindmap));
        }

        // 4. Save changes
        UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                .nodes(mindmap.getNodes())
                .edges(mindmap.getEdges())
                .build();

        MindmapResponse updated = mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);

        // Log what AI did
        for (String logMsg : logs) {
            log.info("[AI Action] {}", logMsg);
        }

        return updated;
    }

    // === Helper Methods ===

    private AiDecision askAiForDecision(Mindmap mindmap, OptimizeRequest request, String lang) {
        List<String> labels = mindmap.getNodes().stream()
                .map(this::extractLabel)
                .filter(StringUtils::hasText)
                .limit(50)
                .collect(Collectors.toList());

        Map<String, Object> payload = promptBuilder.buildClassifyActionPrompt(
                mindmap.getTitle(),
                mindmap.getDescription(),
                labels,
                lang,
                request.getHints());

        // Use streaming to send AI's natural language response to client
        String response = callGeminiStream(payload, String.valueOf(mindmap.getId()));
        return responseParser.parseClassifyResponse(promptBuilder.ensureJson(response));
    }

    private void validatePermissions(Mindmap mindmap, Long userId) {
        boolean isOwner = mindmap.getMysqlUserId() != null && mindmap.getMysqlUserId().equals(userId);
        boolean isEditor = mindmap.getCollaborators() != null && mindmap.getCollaborators().stream()
                .anyMatch(c -> Objects.equals(c.getMysqlUserId(), userId)
                        && "accepted".equals(c.getStatus())
                        && "EDITOR".equals(c.getRole()));

        if (!isOwner && !isEditor) {
            throw new MindmapAccessDeniedException(mindmap.getId(), userId);
        }
    }

    private void deductCredits(Long userId, String mode) {
        if (userId == null)
            return;

        long cost = "max".equalsIgnoreCase(mode) ? 5L
                : ("thinking".equalsIgnoreCase(mode) ? 3L : 1L);

        com.riverflow.model.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long current = user.getCredit() != null ? user.getCredit() : 0L;
        if (current < cost) {
            throw new InvalidMindmapDataException("credit", "Không đủ credit");
        }

        user.setCredit(current - cost);
        userRepository.save(user);
    }

    /**
     * Send realtime event to WebSocket server
     */
    private void sendRealtimeEvent(String mindmapId, String event, Map<String, Object> data) {
        if (realtimeServerUrl == null || realtimeServerUrl.isBlank()) {
            log.warn("Realtime server URL not configured, skipping event: {}", event);
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
                            log.warn("Failed to send realtime event {}: status={}", event, resp.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        log.warn("Error sending realtime event {}: {}", event, ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.warn("Error preparing realtime event {}: {}", event, e.getMessage());
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
            boolean jsonStarted = false;

            // Send streaming start event
            if (mindmapId != null) {
                sendRealtimeEvent(mindmapId, "ai:stream:start", Map.of());
            }

            Flux<Map<String, Object>> responseFlux = geminiWebClient.post()
                    .uri(url)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToFlux((Class<Map>) (Class<?>) Map.class);

            responseFlux.toIterable().forEach(chunk -> {
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
                                    if (!jsonStarted) {
                                        // Check if this chunk starts JSON
                                        if (text.contains("```json") || text.contains("{")) {
                                            jsonStarted = true;
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
                    log.warn("Error processing stream chunk: {}", e.getMessage());
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
            log.error("Gemini streaming API error: {}", e.getMessage());
            if (mindmapId != null) {
                sendRealtimeEvent(mindmapId, "ai:stream:error",
                        Map.of("error", e.getMessage()));
            }
            throw new RuntimeException("AI service failed: " + e.getMessage());
        }
    }

    /**
     * Call Gemini without streaming (for backward compatibility)
     */
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

            List<?> candidates = (List<?>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response");
            }

            Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            List<?> parts = (List<?>) content.get("parts");
            return String.valueOf(((Map<?, ?>) parts.get(0)).get("text"));
        } catch (Exception e) {
            log.error("Gemini API error: {}", e.getMessage());
            throw new RuntimeException("AI service failed: " + e.getMessage());
        }
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("JSON parse error: {}", e.getMessage());
            throw new InvalidMindmapDataException("json", "Invalid AI response");
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        return node.isTextual() ? node.asText() : node.toString();
    }

    private String extractLabel(Map<String, Object> node) {
        Object data = node.get("data");
        if (data instanceof Map<?, ?> m) {
            Object label = m.get("label");
            return label != null ? String.valueOf(label) : null;
        }
        return null;
    }

    private String determineMode(String reqMode) {
        if (reqMode == null || reqMode.isBlank() || "default".equalsIgnoreCase(reqMode)) {
            return "normal";
        }
        return reqMode;
    }
}
