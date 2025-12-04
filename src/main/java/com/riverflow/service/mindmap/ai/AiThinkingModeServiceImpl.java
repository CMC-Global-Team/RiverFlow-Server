package com.riverflow.service.mindmap.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.ai.ThinkingModeRequest;
import com.riverflow.dto.mindmap.ai.ThinkingModeResponse;
import com.riverflow.exception.mindmap.InvalidMindmapDataException;
import com.riverflow.repository.UserRepository;
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
 * Implementation of Thinking Mode Service
 * AI analyzes user prompt and returns optimized mindmap specification
 */
@Service
@RequiredArgsConstructor
public class AiThinkingModeServiceImpl implements AiThinkingModeService {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final GeminiPromptBuilder promptBuilder;

    @Value("${gemini.model:gemini-2.0-flash-exp}")
    private String model;

    @Value("${realtime.server.url:}")
    private String realtimeServerUrl;

    @Override
    public ThinkingModeResponse analyzeAndOptimize(ThinkingModeRequest request, Long userId) {
        return analyzeAndOptimizeWithStreaming(request, userId, null);
    }

    @Override
    public ThinkingModeResponse analyzeAndOptimizeWithStreaming(ThinkingModeRequest request, Long userId, String mindmapId) {
        // Deduct credits for thinking mode (3 credits for deep analysis)
        deductCredits(userId, 3L);

        // Build prompt for Thinking Mode
        Map<String, Object> payload = promptBuilder.buildThinkingModePrompt(
                request.getUserPrompt(),
                request.getLanguage(),
                request.getTags(),
                request.getPreferredStructure(),
                request.getComplexity()
        );

        // Call Gemini with streaming support - use userId for room
        String aiResponse = callGeminiStream(payload, userId);

        // Parse JSON response
        String jsonResponse = promptBuilder.ensureJson(aiResponse);
        JsonNode root = parseJson(jsonResponse);

        // Build response from AI's output
        ThinkingModeResponse response = ThinkingModeResponse.builder()
                .explanation(extractNaturalLanguage(aiResponse))
                .optimizedTopic(textOrNull(root.get("optimizedTopic")))
                .optimizedTitle(textOrNull(root.get("optimizedTitle")))
                .structureType(textOrNull(root.get("structureType")))
                .levels(intOrNull(root.get("levels")))
                .firstLevelCount(intOrNull(root.get("firstLevelCount")))
                .language(textOrNull(root.get("language")))
                .reasoning(textOrNull(root.get("reasoning")))
                .build();

        // Extract tags
        JsonNode tagsNode = root.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            List<String> tags = new ArrayList<>();
            tagsNode.forEach(tag -> tags.add(tag.asText()));
            response.setTags(tags);
        }

        // Extract action list
        JsonNode actionListNode = root.get("actionList");
        System.out.println("[DEBUG ThinkingMode] actionListNode: " + (actionListNode != null ? actionListNode.toString() : "null"));
        if (actionListNode != null && actionListNode.isArray()) {
            List<String> actionList = new ArrayList<>();
            actionListNode.forEach(action -> actionList.add(action.asText()));
            response.setActionList(actionList);
            System.out.println("[DEBUG ThinkingMode] Extracted " + actionList.size() + " actions");
        } else {
            System.out.println("[DEBUG ThinkingMode] No action list found in AI response");
        }

        // Extract additional properties
        JsonNode additionalPropsNode = root.get("additionalProperties");
        if (additionalPropsNode != null && additionalPropsNode.isObject()) {
            Map<String, Object> additionalProps = objectMapper.convertValue(additionalPropsNode, Map.class);
            response.setAdditionalProperties(additionalProps);
        }

        return response;
    }

    /**
     * Deduct credits from user account
     */
    private void deductCredits(Long userId, Long cost) {
        if (userId == null) return;

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
        if (realtimeServerUrl == null || realtimeServerUrl.isBlank() || mindmapId == null) {
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

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // Silently ignore realtime errors
        }
    }

    /**
     * Extract natural language part (before JSON block)
     */
    private String extractNaturalLanguage(String text) {
        if (text == null) return "";

        int jsonStart = text.indexOf("```json");
        if (jsonStart < 0) {
            jsonStart = text.indexOf('{');
        }

        if (jsonStart > 0) {
            return text.substring(0, jsonStart).trim();
        }

        return text.trim();
    }

    /**
     * Call Gemini with streaming support
     * Sends events to user-specific room for reliable delivery
     */
    private String callGeminiStream(Map<String, Object> payload, Long userId) {
        try {
            String url = "/v1beta/models/" + model + ":streamGenerateContent";
            StringBuilder fullText = new StringBuilder();
            StringBuilder naturalLanguagePart = new StringBuilder();
            final boolean[] jsonStarted = { false };

            // Send streaming start event to user room
            if (userId != null) {
                sendRealtimeEventToUser(userId, "ai:thinking:start", Map.of());
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
                                        if (text.contains("```json") || text.contains("{")) {
                                            jsonStarted[0] = true;
                                            String beforeJson = extractNaturalLanguage(text);
                                            if (!beforeJson.isEmpty()) {
                                                naturalLanguagePart.append(beforeJson);
                                                if (userId != null) {
                                                    sendRealtimeEventToUser(userId, "ai:thinking:chunk",
                                                            Map.of("chunk", beforeJson, "done", false));
                                                }
                                            }
                                        } else {
                                            naturalLanguagePart.append(text);
                                            if (userId != null) {
                                                sendRealtimeEventToUser(userId, "ai:thinking:chunk",
                                                        Map.of("chunk", text, "done", false));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue processing
                }
            });

            // Send streaming done event
            if (userId != null) {
                String naturalLangFinal = naturalLanguagePart.toString().trim();
                if (naturalLangFinal.isEmpty()) {
                    naturalLangFinal = extractNaturalLanguage(fullText.toString());
                }
                sendRealtimeEventToUser(userId, "ai:thinking:done",
                        Map.of("fullText", naturalLangFinal));
            }

            return fullText.toString();
        } catch (Exception e) {
            if (userId != null) {
                sendRealtimeEventToUser(userId, "ai:thinking:error",
                        Map.of("error", e.getMessage()));
            }
            throw new RuntimeException("Thinking Mode AI service failed: " + e.getMessage());
        }
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new InvalidMindmapDataException("json", "Invalid AI response from Thinking Mode");
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return node.isTextual() ? node.asText() : node.toString();
    }

    private Integer intOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return node.isInt() ? node.asInt() : null;
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
}
