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
    private final LayoutEngine layoutEngine;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${realtime.server.url:}")
    private String realtimeServerUrl;

    @Override
    public MindmapResponse generateMindmap(GenerateMindmapRequest request, Long userId) {
        // Use Gemini to generate new mindmap from scratch
        String topic = request.getTopic().trim();
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
                topic, levels, firstLevelCount, lang, request.getTags(), mode, minFirst, maxFirst,
                request.getStructureType() != null ? request.getStructureType() : "mindmap");

        String json = callGemini(payload);
        JsonNode root = parseJson(promptBuilder.ensureJson(json));

        // Parse title from AI response, fallback to request title or topic
        String aiTitle = textOrNull(root.get("title"));
        String finalTitle = StringUtils.hasText(aiTitle) ? aiTitle
                : (StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : topic);

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

            // Parse node properties from AI
            String nodeType = textOrNull(n.get("nodeType"));
            String color = textOrNull(n.get("color"));
            String background = textOrNull(n.get("background"));
            String icon = textOrNull(n.get("icon"));
            String description = textOrNull(n.get("description"));
            String shape = textOrNull(n.get("shape"));

            Map<String, Object> data = new HashMap<>();
            data.put("label", label);
            if (StringUtils.hasText(description)) {
                data.put("description", description);
            }
            if (StringUtils.hasText(icon)) {
                data.put("icon", icon);
            }
            if (StringUtils.hasText(shape)) {
                data.put("shape", shape);
            }

            Map<String, Object> style = new HashMap<>();
            if (StringUtils.hasText(background)) {
                style.put("background", background);
                data.put("bgColor", background); // For frontend custom nodes
            }
            if (StringUtils.hasText(color)) {
                style.put("color", color);
                data.put("color", color); // For frontend custom nodes
            }

            // Client supported shapes
            Set<String> supportedShapes = Set.of("rectangle", "circle", "diamond", "hexagon", "ellipse", "roundedRectangle");

            // Determine final node type (shape)
            String finalType = "rectangle"; // Default
            if (StringUtils.hasText(shape) && supportedShapes.contains(shape)) {
                finalType = shape;
            } else if (StringUtils.hasText(nodeType) && supportedShapes.contains(nodeType)) {
                finalType = nodeType;
            }

            Map<String, Object> rfNode = new HashMap<>();
            rfNode.put("id", newId);
            rfNode.put("type", finalType);
            rfNode.put("data", data);
            if (!style.isEmpty()) {
                rfNode.put("style", style);
            }
            rfNode.put("position", Map.of("x", 0, "y", 0));
            rfNodes.add(rfNode);
        }

        // Third pass: create edges
        // Check if AI provided edges directly
        JsonNode edgesNode = root.get("edges");
        if (edgesNode != null && edgesNode.isArray() && edgesNode.size() > 0) {
            // AI provided edges - use them
            for (JsonNode e : edgesNode) {
                String sourceTemp = textOrNull(e.get("source"));
                String targetTemp = textOrNull(e.get("target"));

                if (!StringUtils.hasText(sourceTemp) || !StringUtils.hasText(targetTemp) ||
                        !idMap.containsKey(sourceTemp) || !idMap.containsKey(targetTemp)) {
                    continue;
                }

                String edgeType = textOrNull(e.get("type"));
                String sourceHandle = textOrNull(e.get("sourceHandle"));
                String targetHandle = textOrNull(e.get("targetHandle"));
                String markerEnd = textOrNull(e.get("markerEnd"));
                String label = textOrNull(e.get("label"));
                JsonNode animatedNode = e.get("animated");

                Map<String, Object> edge = new HashMap<>();
                edge.put("id", "edge-" + UUID.randomUUID());
                edge.put("source", idMap.get(sourceTemp));
                edge.put("target", idMap.get(targetTemp));
                edge.put("type", StringUtils.hasText(edgeType) ? edgeType : "smoothstep");
                edge.put("animated",
                        animatedNode != null && animatedNode.isBoolean() ? animatedNode.asBoolean() : true);

                if (StringUtils.hasText(sourceHandle))
                    edge.put("sourceHandle", sourceHandle);
                if (StringUtils.hasText(targetHandle))
                    edge.put("targetHandle", targetHandle);
                if (StringUtils.hasText(markerEnd)) {
                    edge.put("markerEnd", Map.of("type", markerEnd));
                }
                if (StringUtils.hasText(label))
                    edge.put("label", label);

                rfEdges.add(edge);
            }
        } else {
            // AI didn't provide edges - build from parentId relationships with variety
            String[] edgeTypes = { "smoothstep", "step", "straight", "bezier" };
            String[] handles = { "a", "b", "c", "d" };
            int typeIndex = 0;

            for (Map.Entry<String, String> entry : parentByTempId.entrySet()) {
                String childTemp = entry.getKey();
                String parentTemp = entry.getValue();

                if (!StringUtils.hasText(parentTemp) || !idMap.containsKey(parentTemp)
                        || !idMap.containsKey(childTemp)) {
                    continue;
                }

                Map<String, Object> edge = new HashMap<>();
                edge.put("id", "edge-" + UUID.randomUUID());
                edge.put("source", idMap.get(parentTemp));
                edge.put("target", idMap.get(childTemp));
                edge.put("type", edgeTypes[typeIndex % edgeTypes.length]);
                edge.put("animated", typeIndex % 2 == 0);

                // Vary handles for diversity
                if (typeIndex % 3 == 0) {
                    edge.put("sourceHandle", handles[(typeIndex / 2) % handles.length]);
                    edge.put("targetHandle", handles[(typeIndex / 3) % handles.length]);
                }

                // Add markers occasionally
                if (typeIndex % 4 == 0) {
                    edge.put("markerEnd", Map.of("type", "arrowclosed"));
                }

                rfEdges.add(edge);
                typeIndex++;
            }
        }

        // Apply layout based on structure type
        String structureType = request.getStructureType() != null ? request.getStructureType() : "mindmap";
        layoutEngine.applyLayout(structureType, rfNodes, rfEdges);

        // Create mindmap
        CreateMindmapRequest createReq = CreateMindmapRequest.builder()
                .title(finalTitle)
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

        log.info("[AI Optimize] Loaded mindmap: id={}, title={}, nodes={}, edges={}",
                mindmap.getId(), mindmap.getTitle(),
                mindmap.getNodes() != null ? mindmap.getNodes().size() : 0,
                mindmap.getEdges() != null ? mindmap.getEdges().size() : 0);

        validatePermissions(mindmap, userId);

        // 2. Deduct credits based on mode
        String mode = determineMode(request.getMode());
        deductCredits(userId, mode);

        // 3. Ask Gemini AI to analyze and plan operations
        String lang = request.getLanguage() != null ? request.getLanguage() : "vi";
        log.info("[AI Optimize] User hints: {}", request.getHints());

        AiDecision decision = askAiForDecision(mindmap, request, lang);

        log.info("[AI Optimize] AI Decision: targetType={}, hasOps={}, opsCount={}",
                decision.targetType(), decision.hasOps(),
                decision.hasOps() ? decision.ops().size() : 0);

        if (decision.hasOps()) {
            log.info("[AI Optimize] Operations to execute:");
            for (Map<String, Object> op : decision.ops()) {
                log.info("  - Operation: {}", op);
            }
        }

        // 4. Execute AI-decided operations
        List<String> logs = new ArrayList<>();
        if (decision.hasOps()) {
            logs.addAll(operationExecutor.executeOperations(decision.ops(), mindmap));
        } else {
            log.warn("[AI Optimize] No operations generated by AI");
        }

        // 5. Save changes
        log.info("[AI Optimize] Saving changes: nodes={}, edges={}",
                mindmap.getNodes().size(), mindmap.getEdges().size());

        UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                .nodes(mindmap.getNodes())
                .edges(mindmap.getEdges())
                .build();

        MindmapResponse updated = mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);

        // Verify the changes persisted by reloading from database
        Mindmap reloaded = mindmapRepository.findById(mindmap.getId()).orElse(null);
        if (reloaded != null) {
            log.info("[AI Verify] Reloaded mindmap from DB: nodes={}, edges={}",
                    reloaded.getNodes().size(), reloaded.getEdges().size());
            // Log all node labels to verify updates
            for (Map<String, Object> node : reloaded.getNodes()) {
                String label = extractLabel(node);
                String id = String.valueOf(node.get("id"));
                log.info("[AI Verify] Node: id='{}', label='{}'", id, label);
            }
        }

        // Log what AI did
        for (String logMsg : logs) {
            log.info("[AI Action] {}", logMsg);
        }

        log.info("[AI Optimize] Completed successfully");

        return updated;
    }

    // === Helper Methods ===

    private AiDecision askAiForDecision(Mindmap mindmap, OptimizeRequest request, String lang) {
        log.info("[AI Decision] Building prompt for mindmap with {} nodes and {} edges",
                mindmap.getNodes() != null ? mindmap.getNodes().size() : 0,
                mindmap.getEdges() != null ? mindmap.getEdges().size() : 0);

        Map<String, Object> payload = promptBuilder.buildClassifyActionPrompt(
                mindmap.getTitle(),
                mindmap.getDescription(),
                mindmap.getNodes(),
                mindmap.getEdges(),
                lang,
                request.getHints());

        // Use streaming to send AI's natural language response to client
        String response = callGeminiStream(payload, String.valueOf(mindmap.getId()));

        log.info("[AI Decision] Raw AI response length: {} chars", response != null ? response.length() : 0);

        String jsonResponse = promptBuilder.ensureJson(response);
        log.info("[AI Decision] Extracted JSON: {}", jsonResponse);

        AiDecision decision = responseParser.parseClassifyResponse(jsonResponse);
        log.info("[AI Decision] Parsed decision successfully");

        return decision;
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
