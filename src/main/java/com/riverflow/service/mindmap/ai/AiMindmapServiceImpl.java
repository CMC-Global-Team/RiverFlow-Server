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

    // Model and API key (not hardcoded, read from env/properties)
    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    // Generation config (externalized)
    @Value("${ai.generation.temperature:0.3}")
    private double temperature;

    @Value("${ai.generation.max-output-tokens:2048}")
    private int maxOutputTokens;

    @Value("${ai.generation.top-k:40}")
    private int topK;

    @Value("${ai.generation.top-p:0.9}")
    private double topP;

    @Value("${ai.response-mime-type:application/json}")
    private String responseMimeType;

    // Defaults/config for logic (externalized)
    @Value("${ai.lang.default:vi}")
    private String defaultLang;

    @Value("${mindmap.first-level.normal.min:3}")
    private int level1NormalMin;

    @Value("${mindmap.first-level.normal.max:5}")
    private int level1NormalMax;

    @Value("${mindmap.first-level.focus.min:4}")
    private int level1FocusMin;

    @Value("${mindmap.first-level.focus.max:6}")
    private int level1FocusMax;

    @Value("${mindmap.label.min-words:1}")
    private int labelMinWords;

    @Value("${mindmap.label.max-words:4}")
    private int labelMaxWords;

    // Prompts (externalized with safe defaults)
    @Value("${mindmap.prompt.generate.system}")
    private String generateSystemPrompt;

    @Value("${mindmap.prompt.optimize-node.system}")
    private String optimizeNodeSystemPrompt;

    @Value("${mindmap.prompt.optimize-desc.system}")
    private String optimizeDescSystemPrompt;

    @Override
    public MindmapResponse generateMindmap(GenerateMindmapRequest request, Long userId) {
        assertConfigured();

        String topic = request.getTopic().trim();
        String title = StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : topic;
        int levels = request.getLevels() != null ? request.getLevels() : 2;
        String reqMode = request.getMode();
        String mode = (reqMode == null || reqMode.isBlank() || "default".equalsIgnoreCase(reqMode)) ? "normal" : reqMode;

        // configurable first-level range by mode
        int minFirst = "normal".equalsIgnoreCase(mode) ? level1NormalMin : level1FocusMin;
        int maxFirst = "normal".equalsIgnoreCase(mode) ? level1NormalMax : level1FocusMax;

        int requestedFirst = request.getFirstLevelCount() != null ? request.getFirstLevelCount() : minFirst;
        int firstLevelCount = Math.max(minFirst, Math.min(maxFirst, requestedFirst));

        String lang = StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : defaultLang;

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
        // keep original ids set to check duplicates
        Set<String> tempIds = new HashSet<>();

        // collect nodes
        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));
            String parentTempId = textOrNull(n.get("parentId"));
            if (!StringUtils.hasText(tempId) || !StringUtils.hasText(label)) {
                throw new InvalidMindmapDataException("node", "Thiếu id hoặc label");
            }
            if (!tempIds.add(tempId)) {
                throw new InvalidMindmapDataException("id", "Trùng id: " + tempId);
            }
            ensureLabelLength(label);
            parentByTempId.put(tempId, (parentTempId != null && parentTempId.isBlank()) ? null : parentTempId);
        }

        // find roots: parentId == null
        List<String> roots = parentByTempId.entrySet().stream()
                .filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (roots.size() != 1) {
            throw new InvalidMindmapDataException("root", "Mindmap phải có đúng 1 root, hiện có: " + roots.size());
        }
        String rootTempId = roots.get(0);

        // Validate parent references exist
        for (Map.Entry<String, String> e : parentByTempId.entrySet()) {
            String child = e.getKey();
            String parent = e.getValue();
            if (parent != null && !tempIds.contains(parent)) {
                throw new InvalidMindmapDataException("parentId", "parentId không tồn tại cho node: " + child);
            }
        }

        // Validate first level count according to mode range
        long level1Count = parentByTempId.entrySet().stream()
                .filter(e -> rootTempId.equals(e.getValue()))
                .count();
        if (level1Count < minFirst || level1Count > maxFirst) {
            throw new InvalidMindmapDataException("level1", String.format("Số node cấp 1 phải %d–%d", minFirst, maxFirst));
        }

        // Validate depth according to requested levels (root level 0)
        Map<String, Integer> depthById = computeDepths(parentByTempId, rootTempId);
        int maxDepthFound = depthById.values().stream().max(Integer::compareTo).orElse(0);
        if (maxDepthFound > levels) {
            throw new InvalidMindmapDataException("depth", "Độ sâu vượt quá yêu cầu: " + maxDepthFound + ">" + levels);
        }

        // Create ReactFlow nodes and edges
        // Also preserve labels
        Map<String, String> labelByTempId = new HashMap<>();
        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));
            labelByTempId.put(tempId, label);
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
            if (parentTemp == null) continue; // skip root
            String sourceId = idMap.get(parentTemp);
            String targetId = idMap.get(childTemp);
            if (!StringUtils.hasText(sourceId) || !StringUtils.hasText(targetId)) {
                throw new InvalidMindmapDataException("edge", "Không thể tạo cạnh cho node: " + childTemp);
            }
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", newEdgeId());
            edge.put("source", sourceId);
            edge.put("target", targetId);
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
        assertConfigured();

        Mindmap mindmap = mindmapRepository.findById(request.getMindmapId())
                .orElseThrow(() -> new MindmapNotFoundException(request.getMindmapId(), userId));

        boolean isOwner = mindmap.getMysqlUserId() != null && mindmap.getMysqlUserId().equals(userId);
        boolean isEditor = mindmap.getCollaborators() != null && mindmap.getCollaborators().stream()
                .anyMatch(c -> Objects.equals(c.getMysqlUserId(), userId) && "accepted".equals(c.getStatus()) && "EDITOR".equals(c.getRole()));
        if (!isOwner && !isEditor) {
            throw new MindmapAccessDeniedException(request.getMindmapId(), userId);
        }

        String lang = StringUtils.hasText(request.getLanguage()) ? request.getLanguage() : defaultLang;
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
        int words = (int) Arrays.stream(label.trim().split("\\s+")).filter(s -> !s.isBlank()).count();
        if (words < labelMinWords || words > labelMaxWords) {
            throw new InvalidMindmapDataException("label", "Độ dài label phải " + labelMinWords + "–" + labelMaxWords + " từ");
        }
    }

    private void assertConfigured() {
        if (!StringUtils.hasText(geminiApiKey)) {
            throw new IllegalStateException("GEMINI_API_KEY not set");
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
                            .build(model))
                    .header("x-goog-api-key", geminiApiKey)
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
            return ensureJson(s);
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

    private Map<String, Integer> computeDepths(Map<String, String> parentById, String rootId) {
        Map<String, Integer> depth = new HashMap<>();
        Deque<String> dq = new ArrayDeque<>();
        depth.put(rootId, 0);
        dq.add(rootId);
        while (!dq.isEmpty()) {
            String cur = dq.removeFirst();
            int d = depth.get(cur);
            for (Map.Entry<String, String> e : parentById.entrySet()) {
                if (cur.equals(e.getValue())) {
                    String child = e.getKey();
                    if (depth.containsKey(child)) {
                        // cycle detected
                        throw new InvalidMindmapDataException("cycle", "Phát hiện chu kỳ giữa " + cur + " và " + child);
                    }
                    depth.put(child, d + 1);
                    dq.addLast(child);
                }
            }
        }
        // ensure all nodes reached
        if (depth.size() != parentById.size()) {
            throw new InvalidMindmapDataException("disconnected", "Có node không nối với root");
        }
        return depth;
    }

    private Map<String, Object> buildGeminiPayloadForGenerate(String topic, int levels, int firstLevelCount, String lang, List<String> tags, String mode, int minFirst, int maxFirst) {
        String system = generateSystemPrompt;
        StringBuilder user = new StringBuilder();
        user.append("Tạo mindmap về chủ đề: '").append(topic).append("' (ngôn ngữ: ").append(lang).append("). ")
            .append("Độ sâu tối đa: ").append(levels).append(". ")
            .append("Số node cấp 1 mong muốn: ").append(firstLevelCount).append(" (giới hạn ")
            .append(minFirst).append("–").append(maxFirst).append("). MODE: ").append(mode).append(".\n")
            .append("Chỉ trả JSON dạng: {\n  \"nodes\": [\n    {\"id\": \"n1\", \"label\": \"").append(topic).append("\", \"parentId\": null},\n    {\"id\": \"n2\", \"label\": \"...\", \"parentId\": \"n1\"}\n  ]\n}\n")
            .append("Ràng buộc: label 1–").append(labelMaxWords).append(" từ; không lặp; parentId hợp lệ; đúng 1 root; không chu kỳ.");
        if (tags != null && !tags.isEmpty()) {
            user.append("\nGợi ý bổ sung: ").append(String.join(", ", tags));
        }
        String prompt = system + "\n\n" + user;
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("topK", topK);
        generationConfig.put("topP", topP);
        generationConfig.put("response_mime_type", responseMimeType);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    private Map<String, Object> buildGeminiPayloadForOptimizeNode(String currentLabel, List<String> siblingLabels, String lang, List<String> hints) {
        String system = optimizeNodeSystemPrompt;
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

        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", system + "\n\n" + user))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", Math.max(512, Math.min(1024, maxOutputTokens)));
        generationConfig.put("topK", topK);
        generationConfig.put("topP", topP);
        generationConfig.put("response_mime_type", responseMimeType);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    private String ensureJson(String text) {
        if (text == null) return null;
        String s = text.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1).trim();
        }
        return s; // nếu không có { } thì vẫn trả về, parseJson sẽ báo lỗi phù hợp
    }

    private Map<String, Object> buildGeminiPayloadForOptimizeDescription(String title, String currentDesc, String lang, List<String> hints, String mode) {
        String system = optimizeDescSystemPrompt;
        StringBuilder user = new StringBuilder();
        user.append("Ngôn ngữ: ").append(lang).append("\n");
        if (StringUtils.hasText(title)) user.append("Tiêu đề: ").append(title).append("\n");
        if (StringUtils.hasText(currentDesc)) user.append("Mô tả hiện tại: ").append(currentDesc).append("\n");
        if (hints != null && !hints.isEmpty()) {
            user.append("Gợi ý: ").append(String.join(", ", hints)).append("\n");
        }
        user.append("Trả JSON: { \"description\": \"...\" }");

        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", system + "\n\n" + user))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", Math.max(512, Math.min(2048, maxOutputTokens)));
        generationConfig.put("topK", topK);
        generationConfig.put("topP", topP);
        generationConfig.put("response_mime_type", responseMimeType);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    private String newNodeId() {
        return "node-" + UUID.randomUUID();
    }

    private String newEdgeId() {
        return "edge-" + UUID.randomUUID();
    }
}