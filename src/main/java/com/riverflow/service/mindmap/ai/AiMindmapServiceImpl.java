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
    private final AiThinkingModeService thinkingModeService;

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

        // Check if Thinking Mode should be used
        if ("thinking".equalsIgnoreCase(mode)) {
            return generateWithThinkingMode(request, userId);
        }

        int minFirst = "normal".equalsIgnoreCase(mode) ? 3 : 4;
        int maxFirst = "normal".equalsIgnoreCase(mode) ? 5 : 6;
        int defaultFirst = "normal".equalsIgnoreCase(mode) ? 4 : 5;
        int firstLevelCount = request.getFirstLevelCount() != null
                ? Math.max(minFirst, Math.min(maxFirst, request.getFirstLevelCount()))
                : defaultFirst;

        // Deduct credits
        deductCredits(userId, mode);

        // Notify user that generation is starting
        if (userId != null) {
            sendRealtimeEventToUser(userId, "ai:generate:start", Map.of("mode", mode, "topic", topic));
        }

        // Ask Gemini to generate mindmap with streaming
        Map<String, Object> payload = promptBuilder.buildGeneratePrompt(
                topic, levels, firstLevelCount, lang, request.getTags(), mode, minFirst, maxFirst,
                request.getStructureType() != null ? request.getStructureType() : "mindmap");

        // Use streaming for real-time feedback
        String json = callGeminiStreamToUser(payload, userId);
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

                // Don't set handles here - let calculateEdgeHandles() do it based on positions

                // Add markers occasionally
                if (typeIndex % 4 == 0) {
                    edge.put("markerEnd", Map.of("type", "arrowclosed"));
                }

                rfEdges.add(edge);
                typeIndex++;
            }
        }

        // DEBUG: Log edge creation
        System.out.println("=== AI MINDMAP GENERATION DEBUG ===");
        System.out.println("Generated " + rfNodes.size() + " nodes");
        System.out.println("Generated " + rfEdges.size() + " edges");
        if (!rfEdges.isEmpty()) {
            System.out.println("Sample edge BEFORE layout: " + rfEdges.get(0));
        } else {
            System.out.println("WARNING: No edges were created!");
            System.out.println("parentByTempId size: " + parentByTempId.size());
            System.out.println("idMap size: " + idMap.size());
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

        validatePermissions(mindmap, userId);

        // 2. Deduct credits based on mode
        String mode = determineMode(request.getMode());
        deductCredits(userId, mode);

        // 3. For Thinking Mode, use ThinkingModeService to analyze and send action list
        String lang = request.getLanguage() != null ? request.getLanguage() : "vi";
        
        // If thinking mode, get optimized parameters and send action list
        if ("thinking".equalsIgnoreCase(mode) && request.getHints() != null && !request.getHints().isEmpty()) {
            // Use first hint as user prompt for thinking mode
            String userPrompt = request.getHints().get(0);
            
            com.riverflow.dto.mindmap.ai.ThinkingModeRequest thinkingRequest = 
                com.riverflow.dto.mindmap.ai.ThinkingModeRequest.builder()
                    .userPrompt(userPrompt)
                    .language(lang)
                    .preferredStructure(request.getStructureType())
                    .complexity("normal")
                    .build();
            
            // Get thinking mode analysis - DON'T deduct credits again (already deducted above)
            // Pass mindmapId for proper room routing
            com.riverflow.dto.mindmap.ai.ThinkingModeResponse thinkingResult = 
                thinkingModeService.analyzeAndOptimizeWithStreaming(thinkingRequest, null, mindmap.getId());
            
            // Send action list to user
            if (userId != null && thinkingResult.getActionList() != null && !thinkingResult.getActionList().isEmpty()) {
                String actionHeader = lang.equals("vi") ? "**Kế hoạch thực hiện:**\n" : "**Action Plan:**\n";
                String actionListText = actionHeader + 
                    String.join("\n", thinkingResult.getActionList().stream()
                        .map(action -> "- " + action)
                        .toArray(String[]::new));
                
                sendRealtimeEventToUser(userId, "ai:thinking:actionlist", 
                    Map.of("text", actionListText, "actions", thinkingResult.getActionList()));
            }
        }
        
        // 4. Ask Gemini AI to analyze and plan operations
        AiDecision decision = askAiForDecision(mindmap, request, lang);

        if (decision.hasOps()) {
            for (Map<String, Object> op : decision.ops()) {
                }
        }

        // 5. Execute AI-decided operations
        List<String> logs = new ArrayList<>();
        if (decision.hasOps()) {
            logs.addAll(operationExecutor.executeOperations(decision.ops(), mindmap));
        } else {
            }

        // 6. Apply layout to properly position all nodes
        // Use structure type from AI decision, or fall back to request, or default to mindmap
        String structureType = decision.structureType() != null 
                ? decision.structureType() 
                : (request.getStructureType() != null ? request.getStructureType() : "mindmap");
        layoutEngine.applyLayout(structureType, mindmap.getNodes(), mindmap.getEdges());

        // 7. Save changes
        UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                .nodes(mindmap.getNodes())
                .edges(mindmap.getEdges())
                .build();

        MindmapResponse updated = mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);

        // Verify the changes persisted by reloading from database
        Mindmap reloaded = mindmapRepository.findById(mindmap.getId()).orElse(null);
        if (reloaded != null) {
            // Log all node labels to verify updates
            for (Map<String, Object> node : reloaded.getNodes()) {
                String label = extractLabel(node);
                String id = String.valueOf(node.get("id"));
                }
        }

        // Log what AI did
        for (String logMsg : logs) {
            }

        return updated;
    }

    // === Helper Methods ===

    private AiDecision askAiForDecision(Mindmap mindmap, OptimizeRequest request, String lang) {
        Map<String, Object> payload = promptBuilder.buildClassifyActionPrompt(
                mindmap.getTitle(),
                mindmap.getDescription(),
                mindmap.getNodes(),
                mindmap.getEdges(),
                lang,
                request.getHints(),
                request.getStructureType());

        // Use streaming to send AI's natural language response to client
        String response = callGeminiStream(payload, String.valueOf(mindmap.getId()));

        String jsonResponse = promptBuilder.ensureJson(response);
        AiDecision decision = responseParser.parseClassifyResponse(jsonResponse);
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

    /**
     * Generate mindmap using Thinking Mode
     * Flow: User Prompt -> Thinking Mode (optimize) -> Agent (decide) -> Generator (create)
     */
    private MindmapResponse generateWithThinkingMode(GenerateMindmapRequest request, Long userId) {
        String topic = request.getTopic().trim();
        String lang = request.getLanguage() != null ? request.getLanguage() : "vi";

        // Step 1: Use Thinking Mode to analyze and optimize the prompt
        com.riverflow.dto.mindmap.ai.ThinkingModeRequest thinkingRequest = 
            com.riverflow.dto.mindmap.ai.ThinkingModeRequest.builder()
                .userPrompt(topic)
                .language(lang)
                .tags(request.getTags())
                .preferredStructure(request.getStructureType())
                .complexity("normal")
                .build();

        // Thinking Mode will deduct its own credits (3 credits total) and stream to user
        // Pass "temp" as mindmapId for now since we're generating a new mindmap
        // The streaming will still work for sending explanations to the client
                com.riverflow.dto.mindmap.ai.ThinkingModeResponse optimized = 
            thinkingModeService.analyzeAndOptimizeWithStreaming(thinkingRequest, userId, "thinking-gen-" + System.currentTimeMillis());

        // Send action list as a separate message to show the plan
        if (userId != null && optimized.getActionList() != null && !optimized.getActionList().isEmpty()) {
            System.out.println("[DEBUG] Sending action list with " + optimized.getActionList().size() + " actions");
            String actionHeader = lang.equals("vi") ? "**Kế hoạch thực hiện:**\n" : "**Action Plan:**\n";
            String actionListText = actionHeader + 
                String.join("\n", optimized.getActionList().stream()
                    .map(action -> "- " + action)
                    .toArray(String[]::new));
            
            sendRealtimeEventToUser(userId, "ai:thinking:actionlist", 
                Map.of("text", actionListText, "actions", optimized.getActionList()));
            System.out.println("[DEBUG] Action list event sent to user:" + userId);
        } else {
            System.out.println("[DEBUG] Action list not sent - userId=" + userId + 
                ", actionList=" + (optimized.getActionList() != null ? optimized.getActionList().size() : "null"));
        }

        // Step 2: Agent decides based on optimized spec
        // Use optimized parameters from Thinking Mode
        String optimizedTopic = optimized.getOptimizedTopic() != null 
            ? optimized.getOptimizedTopic() : topic;
        String optimizedTitle = optimized.getOptimizedTitle() != null 
            ? optimized.getOptimizedTitle() : request.getTitle();
        String structureType = optimized.getStructureType() != null 
            ? optimized.getStructureType() : "mindmap";
        int levels = optimized.getLevels() != null 
            ? optimized.getLevels() : (request.getLevels() != null ? request.getLevels() : 2);
        int firstLevelCount = optimized.getFirstLevelCount() != null 
            ? optimized.getFirstLevelCount() : (request.getFirstLevelCount() != null ? request.getFirstLevelCount() : 5);
        List<String> tags = optimized.getTags() != null 
            ? optimized.getTags() : request.getTags();

        // NO extra credit deduction - Thinking Mode already deducted 3 credits
        // This makes the total cost 3 credits (not 4)

        // Step 3: Generate mindmap with optimized parameters
        int minFirst = 3;
        int maxFirst = 6;
        Map<String, Object> payload = promptBuilder.buildGeneratePrompt(
                optimizedTopic, levels, firstLevelCount, lang, tags, "normal", minFirst, maxFirst, structureType);

        String json = callGemini(payload);
        JsonNode root = parseJson(promptBuilder.ensureJson(json));

        // Parse and use optimized title
        String aiTitle = textOrNull(root.get("title"));
        String finalTitle = StringUtils.hasText(aiTitle) ? aiTitle
                : (StringUtils.hasText(optimizedTitle) ? optimizedTitle : optimizedTopic);

        // Continue with standard mindmap generation flow
        JsonNode nodesNode = root.get("nodes");
        if (nodesNode == null || !nodesNode.isArray()) {
            throw new InvalidMindmapDataException("nodes", "AI didn't return valid nodes array");
        }

        // Build ReactFlow structure (same as normal generation)
        List<Map<String, Object>> rfNodes = new ArrayList<>();
        List<Map<String, Object>> rfEdges = new ArrayList<>();
        Map<String, String> idMap = new HashMap<>();
        Map<String, String> parentByTempId = new HashMap<>();

        // Process nodes and edges (reusing existing logic)
        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));
            String parentTempId = textOrNull(n.get("parentId"));

            if (!StringUtils.hasText(tempId) || !StringUtils.hasText(label)) {
                continue;
            }
            parentByTempId.put(tempId, parentTempId);
        }

        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));

            if (!StringUtils.hasText(tempId) || !StringUtils.hasText(label)) {
                continue;
            }

            String newId = "node-" + UUID.randomUUID();
            idMap.put(tempId, newId);

            String nodeType = textOrNull(n.get("nodeType"));
            String color = textOrNull(n.get("color"));
            String background = textOrNull(n.get("background"));
            String icon = textOrNull(n.get("icon"));
            String description = textOrNull(n.get("description"));
            String shape = textOrNull(n.get("shape"));

            Map<String, Object> data = new HashMap<>();
            data.put("label", label);
            if (StringUtils.hasText(description)) data.put("description", description);
            if (StringUtils.hasText(icon)) data.put("icon", icon);
            if (StringUtils.hasText(shape)) data.put("shape", shape);

            Map<String, Object> style = new HashMap<>();
            if (StringUtils.hasText(background)) {
                style.put("background", background);
                data.put("bgColor", background);
            }
            if (StringUtils.hasText(color)) {
                style.put("color", color);
                data.put("color", color);
            }

            Set<String> supportedShapes = Set.of("rectangle", "circle", "diamond", "hexagon", "ellipse", "roundedRectangle");
            String finalType = "rectangle";
            if (StringUtils.hasText(shape) && supportedShapes.contains(shape)) {
                finalType = shape;
            } else if (StringUtils.hasText(nodeType) && supportedShapes.contains(nodeType)) {
                finalType = nodeType;
            }

            Map<String, Object> rfNode = new HashMap<>();
            rfNode.put("id", newId);
            rfNode.put("type", finalType);
            rfNode.put("data", data);
            if (!style.isEmpty()) rfNode.put("style", style);
            rfNode.put("position", Map.of("x", 0, "y", 0));
            rfNodes.add(rfNode);
        }

        // Create edges
        JsonNode edgesNode = root.get("edges");
        if (edgesNode != null && edgesNode.isArray() && edgesNode.size() > 0) {
            for (JsonNode e : edgesNode) {
                String sourceTemp = textOrNull(e.get("source"));
                String targetTemp = textOrNull(e.get("target"));

                if (!StringUtils.hasText(sourceTemp) || !StringUtils.hasText(targetTemp) ||
                        !idMap.containsKey(sourceTemp) || !idMap.containsKey(targetTemp)) {
                    continue;
                }

                Map<String, Object> edge = new HashMap<>();
                edge.put("id", "edge-" + UUID.randomUUID());
                edge.put("source", idMap.get(sourceTemp));
                edge.put("target", idMap.get(targetTemp));
                edge.put("type", StringUtils.hasText(textOrNull(e.get("type"))) ? textOrNull(e.get("type")) : "smoothstep");
                edge.put("animated", true);
                rfEdges.add(edge);
            }
        } else {
            // Build edges from parentId relationships
            String[] edgeTypes = { "smoothstep", "step", "straight", "bezier" };
            int typeIndex = 0;
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
                edge.put("type", edgeTypes[typeIndex % edgeTypes.length]);
                edge.put("animated", typeIndex % 2 == 0);
                rfEdges.add(edge);
                typeIndex++;
            }
        }

        // Apply layout
        layoutEngine.applyLayout(structureType, rfNodes, rfEdges);

        // Create mindmap
        CreateMindmapRequest createReq = CreateMindmapRequest.builder()
                .title(finalTitle)
                .nodes(rfNodes)
                .edges(rfEdges)
                .aiGenerated(true)
                .category("ai-generated-thinking")
                .build();

        return mindmapService.createMindmap(createReq, userId);
    }

    private void deductCredits(Long userId, String mode) {
        if (userId == null)
            return;

        // Determine credit cost based on mode
        long cost;
        if ("max".equalsIgnoreCase(mode)) {
            cost = 5L;
        } else if ("thinking".equalsIgnoreCase(mode)) {
            cost = 3L; // Thinking mode costs 3 credits
        } else {
            cost = 1L; // Normal mode costs 1 credit
        }

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
     * Supports both mindmap-based and user-based rooms
     */
    private void sendRealtimeEvent(String mindmapId, String event, Map<String, Object> data) {
        if (realtimeServerUrl == null || realtimeServerUrl.isBlank()) {
            return;
        }
        try {
            String room = mindmapId != null ? "mindmap:" + mindmapId : null;
            Map<String, Object> payload = new HashMap<>();
            if (room != null) {
                payload.put("room", room);
            }
            payload.put("event", event);
            payload.put("data", data != null ? data : Map.of());

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(realtimeServerUrl + "/realtime/mindmap/event"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // Silently ignore realtime errors
        }
    }

    /**
     * Send realtime event to user-specific room
     */
    private void sendRealtimeEventToUser(Long userId, String event, Map<String, Object> data) {
        if (realtimeServerUrl == null || realtimeServerUrl.isBlank() || userId == null) {
            return;
        }
        try {
            String room = "user:" + userId;
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

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // Silently ignore realtime errors
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

    /**
     * Call Gemini with streaming to user room (avoids duplicates)
     * Used for generation where we want real-time token streaming
     */
    /**
     * Split large chunk into smaller pieces and send with delay for smoother streaming
     * This simulates token-by-token streaming like ChatGPT
     */
    private void sendChunksWithDelay(Long userId, String fullChunk, String eventName) {
        if (fullChunk == null || fullChunk.isEmpty() || userId == null) {
            return;
        }

        // Split into smaller chunks (approximately 3-5 characters per chunk for smooth effect)
        int chunkSize = 4; // Adjust this for faster/slower streaming
        for (int i = 0; i < fullChunk.length(); i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, fullChunk.length());
            String miniChunk = fullChunk.substring(i, endIndex);
            
            sendRealtimeEventToUser(userId, eventName, Map.of("chunk", miniChunk, "done", false));
            
            // Small delay between chunks for smoother visual effect (optional)
            try {
                Thread.sleep(5); // 5ms delay - adjust for speed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private String callGeminiStreamToUser(Map<String, Object> payload, Long userId) {
        if (userId == null) {
            // Fallback to non-streaming if no user
            System.out.println("[AI Stream] WARNING: No userId provided, falling back to non-streaming");
            return callGemini(payload);
        }

        try {
            String url = "/v1beta/models/" + model + ":streamGenerateContent";
            StringBuilder fullText = new StringBuilder();
            final java.util.concurrent.atomic.AtomicInteger chunkCount = new java.util.concurrent.atomic.AtomicInteger(0);

            // Send streaming start event
            System.out.println("[AI Stream] Starting stream for userId: " + userId);
            sendRealtimeEventToUser(userId, "ai:stream:start", Map.of());

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
                                if (text != null && !"null".equals(text) && !text.isEmpty()) {
                                    fullText.append(text);
                                    int count = chunkCount.incrementAndGet();
                                    System.out.println("[AI Stream] Chunk " + count + " (length: " + text.length() + ") -> splitting for user:" + userId);
                                    
                                    // Split large chunk into smaller pieces for smoother streaming
                                    sendChunksWithDelay(userId, text, "ai:stream:chunk");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[AI Stream] Error processing chunk: " + e.getMessage());
                    // Continue processing
                }
            });

            // Send done event
            System.out.println("[AI Stream] Stream complete. Total chunks: " + chunkCount.get() + ", Total length: " + fullText.length());
            sendRealtimeEventToUser(userId, "ai:stream:done", Map.of("done", true));

            return fullText.toString();
        } catch (Exception e) {
            System.out.println("[AI Stream] Fatal error: " + e.getMessage());
            sendRealtimeEventToUser(userId, "ai:stream:error",
                    Map.of("error", e.getMessage()));
            throw new RuntimeException("AI generation failed: " + e.getMessage(), e);
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
            throw new RuntimeException("AI service failed: " + e.getMessage());
        }
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
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
