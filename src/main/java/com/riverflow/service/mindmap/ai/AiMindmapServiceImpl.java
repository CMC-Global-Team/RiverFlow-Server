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
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.service.mindmap.MindmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMindmapServiceImpl implements AiMindmapService {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient; // configured Gemini client
    private final MindmapRepository mindmapRepository;
    private final MindmapService mindmapService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Override
    public MindmapResponse generateMindmap(GenerateMindmapRequest request, Long userId) {
        String topic = request.getTopic().trim();
        String title = StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : topic;
        int levels = request.getLevels() != null ? request.getLevels() : 2;
        String reqMode = request.getMode();
        String mode = (reqMode == null || reqMode.isBlank() || "default".equalsIgnoreCase(reqMode)) ? "normal" : reqMode;
        int defaultFirst = "normal".equalsIgnoreCase(mode) ? 4 : 5;
        int reqFirst = request.getFirstLevelCount() != null ? request.getFirstLevelCount() : defaultFirst;
        String lang = request.getLanguage() != null ? request.getLanguage() : "vi";

        int minFirst = "normal".equalsIgnoreCase(mode) ? 3 : 4;
        int maxFirst = "normal".equalsIgnoreCase(mode) ? 5 : 6;
        int firstLevelCount = Math.max(minFirst, Math.min(maxFirst, reqFirst));

        Map<String, Object> payload = buildGeminiPayloadForGenerate(topic, levels, firstLevelCount, lang, request.getTags(), mode, minFirst, maxFirst);

        String json = callGemini(payload);
        JsonNode root = parseJson(json);

        // Validate schema: nodes array with parent-child, root must exist
        JsonNode nodesNode = root.get("nodes");
        if (nodesNode == null || !nodesNode.isArray()) {
            throw new InvalidMindmapDataException("nodes", "Phải là mảng node hợp lệ");
        }

        // Build node graph
        List<Map<String, Object>> rfNodes = new ArrayList<>();
        List<Map<String, Object>> rfEdges = new ArrayList<>();

        // tempId to newId mapping
        Map<String, String> idMap = new HashMap<>();
        // parent relations by tempId
        Map<String, String> parentByTempId = new HashMap<>();

        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));
            String parentTempId = textOrNull(n.get("parentId"));
            if (!StringUtils.hasText(tempId) || !StringUtils.hasText(label)) {
                throw new InvalidMindmapDataException("node", "Thiếu id hoặc label");
            }
            ensureLabelLength(label);
            parentByTempId.put(tempId, parentTempId);
        }

        // find root(s): parentId == null
        List<String> roots = parentByTempId.entrySet().stream()
                .filter(e -> e.getValue() == null || e.getValue().isBlank())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (roots.isEmpty()) throw new InvalidMindmapDataException("root", "Không có root node");
        String rootTempId = roots.get(0);

        // Validate first level count according to mode range
        long level1Count = parentByTempId.entrySet().stream()
                .filter(e -> rootTempId.equals(e.getValue()))
                .count();
        if (level1Count < minFirst || level1Count > maxFirst) {
            throw new InvalidMindmapDataException("level1", String.format("Số node cấp 1 phải %d–%d", minFirst, maxFirst));
        }

        // Create ReactFlow nodes and edges
        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));
            String newId = newNodeId();
            idMap.put(tempId, newId);
            Map<String, Object> data = new HashMap<>();
            data.put("label", label);

            Map<String, Object> position = new HashMap<>();
            position.put("x", 0);
            position.put("y", 0);

            Map<String, Object> rfNode = new HashMap<>();
            rfNode.put("id", newId);
            rfNode.put("type", "default");
            rfNode.put("data", data);
            rfNode.put("position", position);
            rfNodes.add(rfNode);
        }

        // edges
        for (Map.Entry<String, String> e : parentByTempId.entrySet()) {
            String childTemp = e.getKey();
            String parentTemp = e.getValue();
            if (parentTemp == null || parentTemp.isBlank()) continue;
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", newEdgeId());
            edge.put("source", idMap.get(parentTemp));
            edge.put("target", idMap.get(childTemp));
            rfEdges.add(edge);
        }

        CreateMindmapRequest createReq = CreateMindmapRequest.builder()
                .title(title)
                .description(null)
                .nodes(rfNodes)
                .edges(rfEdges)
                .aiGenerated(true)
                .category("ai-generated")
                .build();

        MindmapResponse created = mindmapService.createMindmap(createReq, userId);
        return created;
    }

    @Override
    public MindmapResponse optimize(OptimizeRequest request, Long userId) {
        Mindmap mindmap = mindmapRepository.findById(request.getMindmapId())
                .orElseThrow(() -> new MindmapNotFoundException(request.getMindmapId(), userId));

        boolean isOwner = mindmap.getMysqlUserId() != null && mindmap.getMysqlUserId().equals(userId);
        boolean isEditor = mindmap.getCollaborators() != null && mindmap.getCollaborators().stream()
                .anyMatch(c -> Objects.equals(c.getMysqlUserId(), userId) && "accepted".equals(c.getStatus()) && "EDITOR".equals(c.getRole()));
        if (!isOwner && !isEditor) {
            throw new MindmapAccessDeniedException(request.getMindmapId(), userId);
        }

        String lang = request.getLanguage() != null ? request.getLanguage() : "vi";
        String target = request.getTargetType();

        if ("node".equalsIgnoreCase(target)) {
            if (!StringUtils.hasText(request.getNodeId())) {
                throw new InvalidMindmapDataException("nodeId", "nodeId bắt buộc khi tối ưu node");
            }
            // locate node
            Map<String, Object> node = mindmap.getNodes().stream()
                    .filter(n -> request.getNodeId().equals(String.valueOf(n.get("id"))))
                    .findFirst()
                    .orElseThrow(() -> new MindmapNotFoundException("Node không tồn tại trong mindmap", userId));

            String currentLabel = extractLabel(node);
            // collect sibling labels to avoid duplicates
            List<String> siblingLabels = findSiblingLabels(mindmap, request.getNodeId());

            Map<String, Object> payload = buildGeminiPayloadForOptimizeNode(currentLabel, siblingLabels, lang, request.getHints());
            String json = callGemini(payload);
            JsonNode root = parseJson(json);
            JsonNode labelNode = root.get("label");
            String newLabel = labelNode != null && !labelNode.isNull() ? labelNode.asText() : null;
            if (!StringUtils.hasText(newLabel)) {
                throw new InvalidMindmapDataException("label", "AI không trả label hợp lệ");
            }
            ensureLabelLength(newLabel);
            if (siblingLabels.stream().anyMatch(s -> s.equalsIgnoreCase(newLabel))) {
                throw new InvalidMindmapDataException("label", "Label trùng với nhánh khác");
            }
            // apply update
            Object dataObj = node.get("data");
            Map<String, Object> dataMap;
            if (dataObj instanceof Map<?,?> m) {
                //noinspection unchecked
                dataMap = (Map<String, Object>) m;
            } else {
                dataMap = new HashMap<>();
                node.put("data", dataMap);
            }
            dataMap.put("label", newLabel);

            UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                    .nodes(mindmap.getNodes())
                    .edges(mindmap.getEdges())
                    .build();
            return mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);
        } else if ("description".equalsIgnoreCase(target)) {
            String currentDesc = mindmap.getDescription();
            String title = mindmap.getTitle();
            Map<String, Object> payload = buildGeminiPayloadForOptimizeDescription(title, currentDesc, lang, request.getHints(), "normal");
            String json = callGemini(payload);
            JsonNode root = parseJson(json);
            JsonNode descNode = root.get("description");
            String newDesc = descNode != null && !descNode.isNull() ? descNode.asText() : null;
            if (!StringUtils.hasText(newDesc)) {
                throw new InvalidMindmapDataException("description", "AI không trả description hợp lệ");
            }

            UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                    .description(newDesc)
                    .build();
            return mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);
        } else {
            throw new InvalidMindmapDataException("targetType", "Giá trị không hợp lệ: node|description");
        }
    }

    private void ensureLabelLength(String label) {
        int words = Arrays.stream(label.trim().split("\\s+")).filter(s -> !s.isBlank()).toArray().length;
        if (words < 1 || words > 4) {
            throw new InvalidMindmapDataException("label", "Độ dài label phải 1–4 từ");
        }
    }

    private String extractLabel(Map<String, Object> node) {
        Object data = node.get("data");
        if (data instanceof Map<?,?> m) {
            Object lbl = m.get("label");
            return lbl != null ? String.valueOf(lbl) : null;
        }
        return null;
    }

    private List<String> findChildrenLabels(Mindmap map, String parentId) {
        Set<String> childIds = map.getEdges().stream()
                .filter(e -> Objects.equals(String.valueOf(e.get("source")), parentId))
                .map(e -> String.valueOf(e.get("target")))
                .collect(Collectors.toSet());
        return map.getNodes().stream()
            .filter(n -> childIds.contains(String.valueOf(n.get("id"))))
            .map(this::extractLabel)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private List<String> findSiblingLabels(Mindmap map, String nodeId) {
        // find parent of this node via edges
        Optional<String> parentIdOpt = map.getEdges().stream()
                .filter(e -> Objects.equals(String.valueOf(e.get("target")), nodeId))
                .map(e -> String.valueOf(e.get("source")))
                .findFirst();
        if (parentIdOpt.isEmpty()) return Collections.emptyList();
        String parentId = parentIdOpt.get();
        // children of same parent
        Set<String> childIds = map.getEdges().stream()
                .filter(e -> Objects.equals(String.valueOf(e.get("source")), parentId))
                .map(e -> String.valueOf(e.get("target")))
                .collect(Collectors.toSet());
        return map.getNodes().stream()
                .filter(n -> childIds.contains(String.valueOf(n.get("id"))))
                .map(this::extractLabel)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return node.isTextual() ? node.asText() : node.toString();
    }

    private String callGemini(Map<String, Object> payload) {
        try {
            Map<?, ?> resp = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/models/{model}:generateContent")
                            .queryParam("key", geminiApiKey)
                            .build(model))
                    .body(BodyInserters.fromValue(payload))
                    .exchangeToMono(clientResponse -> {
                        if (clientResponse.statusCode().is2xxSuccessful()) {
                            return clientResponse.bodyToMono(Map.class);
                        } else {
                            return clientResponse.bodyToMono(String.class).map(body -> {
                                int code = clientResponse.statusCode().value();
                                log.error("Gemini API error ({}): {}", code, body);
                                throw new com.riverflow.exception.AiUpstreamException(code, parseOpenAiError(body, code));
                            });
                        }
                    })
                    .block();
            if (resp == null) throw new IllegalStateException("Empty response from Gemini");
            // Gemini response: candidates[0].content.parts[].text
            List<?> candidates = (List<?>) resp.get("candidates");
            if (candidates == null || candidates.isEmpty()) throw new IllegalStateException("No candidates from Gemini");
            Map<?, ?> c0 = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) c0.get("content");
            if (content == null) throw new IllegalStateException("No content in candidate");
            List<?> parts = (List<?>) content.get("parts");
            if (parts == null || parts.isEmpty()) throw new IllegalStateException("No parts in content");
            Map<?, ?> p0 = (Map<?, ?>) parts.get(0);
            Object text = p0.get("text");
            if (!(text instanceof String s)) throw new IllegalStateException("Invalid Gemini content");
            return s.trim();
        } catch (com.riverflow.exception.AiUpstreamException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage());
            throw new IllegalArgumentException("Không thể gọi AI vào lúc này, vui lòng thử lại.");
        }
    }

    private String parseOpenAiError(String body, int status) {
        try {
            JsonNode n = objectMapper.readTree(body);
            JsonNode err = n.get("error");
            if (err != null) {
                JsonNode msg = err.get("message");
                if (msg != null && msg.isTextual()) {
                    return "AI lỗi (" + status + "): " + msg.asText();
                }
            }
        } catch (Exception ignore) { }
        return "AI lỗi (" + status + ")";
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("AI trả về không phải JSON hợp lệ: {}", e.getMessage());
            throw new InvalidMindmapDataException("output", "AI không trả JSON hợp lệ");
        }
    }

    private Map<String, Object> buildGeminiPayloadForGenerate(String topic, int levels, int firstLevelCount, String lang, List<String> tags, String mode, int minFirst, int maxFirst) {
        String system = "Bạn là công cụ tạo mindmap. Trả về JSON hợp lệ, KHÔNG văn bản thừa. Quy tắc: label 1–4 từ; số node cấp 1 theo chỉ định; node con đúng ngữ nghĩa; không lặp; không giải thích.";
        String user = String.format("Tạo mindmap về chủ đề: '%s' (ngôn ngữ: %s). Độ sâu: %d. Số node cấp 1: %d (giới hạn %d–%d theo MODE: %s).\n" +
                "Trả về JSON dạng:\n" +
                "{\n  \"nodes\": [\n    {\"id\": \"n1\", \"label\": \"%s\", \"parentId\": null},\n    {\"id\": \"n2\", \"label\": \"...\", \"parentId\": \"n1\"}\n  ]\n}\n" +
                "Yêu cầu:\n- label ngắn (1–4 từ).\n- %d–%d node cấp 1 (parentId = id của root).\n- Chỉ trả JSON, không kèm giải thích.",
                topic, lang, levels, firstLevelCount, minFirst, maxFirst, mode, topic, minFirst, maxFirst);
        if (tags != null && !tags.isEmpty()) {
            user += "\nGợi ý bổ sung: " + String.join(", ", tags);
        }
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system))
        );
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);
        generationConfig.put("response_mime_type", "application/json");

        Map<String, Object> payload = new HashMap<>();
        payload.put("system_instruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generation_config", generationConfig);
        return payload;
    }

    private Map<String, Object> buildGeminiPayloadForExpand(String parentLabel, List<String> siblingLabels, int childrenCount, String lang, List<String> hints) {
        String system = "Bạn là công cụ mở rộng node mindmap. Trả JSON hợp lệ, không có văn bản thừa. Quy tắc: label 1–4 từ; đúng ngữ nghĩa với node cha; không trùng với node anh em.";
        StringBuilder user = new StringBuilder();
        user.append("Mở rộng node: '").append(parentLabel).append("' (ngôn ngữ: ").append(lang).append(")\n");
        user.append("Số con cần tạo: ").append(childrenCount).append(".\n");
        if (siblingLabels != null && !siblingLabels.isEmpty()) {
            user.append("Các nhánh đã có: ").append(String.join(", ", siblingLabels)).append("\n");
        }
        if (hints != null && !hints.isEmpty()) {
            user.append("Gợi ý bổ sung: ").append(String.join(", ", hints)).append("\n");
        }
        user.append("Trả JSON dạng: { \"children\": [ { \"label\": \"...\" } ] }\n");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system))
        );
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString()))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.6);
        generationConfig.put("response_mime_type", "application/json");

        Map<String, Object> payload = new HashMap<>();
        payload.put("system_instruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generation_config", generationConfig);
        return payload;
    }

    private Map<String, Object> buildGeminiPayloadForOptimizeNode(String currentLabel, List<String> siblingLabels, String lang, List<String> hints) {
        String system = "Bạn là công cụ tối ưu label của node mindmap. Chỉ trả JSON, không giải thích. Quy tắc: label 1–4 từ, rõ ràng, không trùng với các nhánh anh em, không thêm ký tự thừa.";
        StringBuilder user = new StringBuilder();
        user.append("Ngôn ngữ: ").append(lang).append("\n");
        user.append("Label hiện tại: ").append(currentLabel).append("\n");
        if (siblingLabels != null && !siblingLabels.isEmpty()) {
            user.append("Các nhánh anh em: ").append(String.join(", ", siblingLabels)).append("\n");
        }
        if (hints != null && !hints.isEmpty()) {
            user.append("Gợi ý: ").append(String.join(", ", hints)).append("\n");
        }
        user.append("Trả JSON: { \"label\": \"...\" }");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system))
        );
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString()))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);
        generationConfig.put("response_mime_type", "application/json");

        Map<String, Object> payload = new HashMap<>();
        payload.put("system_instruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generation_config", generationConfig);
        return payload;
    }

    private Map<String, Object> buildGeminiPayloadForOptimizeDescription(String title, String currentDesc, String lang, List<String> hints, String mode) {
        String system = "Bạn là công cụ tối ưu mô tả mindmap. Chỉ trả JSON, không giải thích. MODE normal: ưu tiên tốc độ, mô tả ngắn gọn 1–2 câu, rõ mục tiêu và phạm vi.";
        StringBuilder user = new StringBuilder();
        user.append("Ngôn ngữ: ").append(lang).append("\n");
        if (StringUtils.hasText(title)) user.append("Tiêu đề: ").append(title).append("\n");
        if (StringUtils.hasText(currentDesc)) user.append("Mô tả hiện tại: ").append(currentDesc).append("\n");
        if (hints != null && !hints.isEmpty()) {
            user.append("Gợi ý: ").append(String.join(", ", hints)).append("\n");
        }
        user.append("Trả JSON: { \"description\": \"...\" }");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system))
        );
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString()))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);
        generationConfig.put("response_mime_type", "application/json");

        Map<String, Object> payload = new HashMap<>();
        payload.put("system_instruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generation_config", generationConfig);
        return payload;
    }

    private String newNodeId() {
        return "node-" + UUID.randomUUID();
    }

    private String newEdgeId() {
        return "edge-" + UUID.randomUUID();
    }
}
