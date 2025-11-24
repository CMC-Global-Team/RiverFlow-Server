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

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${realtime.server.url:}")
    private String realtimeServerUrl;

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
        // sanitized labels by tempId
        Map<String, String> labelByTempId = new HashMap<>();

        for (JsonNode n : nodesNode) {
            String tempId = textOrNull(n.get("id"));
            String label = textOrNull(n.get("label"));
            String parentTempId = textOrNull(n.get("parentId"));
            if (!StringUtils.hasText(tempId) || !StringUtils.hasText(label)) {
                throw new InvalidMindmapDataException("node", "Thiếu id hoặc label");
            }
            // ensureLabelLength(label);
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
        List<String> agentLogs = new ArrayList<>();
        Mindmap mindmap = mindmapRepository.findById(request.getMindmapId())
                .orElseThrow(() -> new MindmapNotFoundException(request.getMindmapId(), userId));

        boolean isOwner = mindmap.getMysqlUserId() != null && mindmap.getMysqlUserId().equals(userId);
        boolean isEditor = mindmap.getCollaborators() != null && mindmap.getCollaborators().stream()
                .anyMatch(c -> Objects.equals(c.getMysqlUserId(), userId) && "accepted".equals(c.getStatus()) && "EDITOR".equals(c.getRole()));
        if (!isOwner && !isEditor) {
            throw new MindmapAccessDeniedException(request.getMindmapId(), userId);
        }

        String lang = request.getLanguage() != null ? request.getLanguage() : "vi";
        Map<String, Object> agentPlan = extractAgentPlanFromHints(request.getHints());
        if (agentPlan != null) {
            agentLogs.add("Agent Plan: " + agentPlan.getOrDefault("summary", agentPlan));
        }
        String title = mindmap.getTitle();
        String desc = mindmap.getDescription();
        List<String> labels = mindmap.getNodes().stream()
                .map(this::extractLabel)
                .filter(StringUtils::hasText)
                .limit(50)
                .collect(Collectors.toList());
        Map<String, Object> agentPayload = buildGeminiPayloadForClassifyAction(title, desc, labels, lang, request.getHints());
        String agentJson = callGemini(agentPayload);
        JsonNode agentRoot = parseJson(agentJson);
        String decidedTarget = textOrNull(agentRoot.get("targetType"));
        String nodeLabel = textOrNull(agentRoot.get("nodeLabel"));
        String sType = textOrNull(agentRoot.get("structureType"));
        String outLang = textOrNull(agentRoot.get("language"));
        List<Map<String, Object>> agentOps = new ArrayList<>();
        JsonNode opsNode = agentRoot.get("ops");
        if (opsNode != null && opsNode.isArray()) {
            for (JsonNode op : opsNode) {
                try {
                    Map<String, Object> m = objectMapper.convertValue(op, Map.class);
                    agentOps.add(m);
                } catch (Exception ignored) {}
            }
        }
        if (StringUtils.hasText(decidedTarget)) {
            request.setTargetType(decidedTarget);
        } else if (!StringUtils.hasText(request.getTargetType())) {
            request.setTargetType("structure");
        }
        if (StringUtils.hasText(nodeLabel) && !StringUtils.hasText(request.getNodeId())) {
            String foundId = findNodeIdByLabel(mindmap, nodeLabel);
            if (StringUtils.hasText(foundId)) request.setNodeId(foundId);
        }
        if (StringUtils.hasText(sType) && request.getStructureType() == null) {
            request.setStructureType(sType);
        }
        if (StringUtils.hasText(outLang) && request.getLanguage() == null) {
            request.setLanguage(outLang);
        }
        agentLogs.add("Agent Analyze: target=" + request.getTargetType() + (StringUtils.hasText(request.getNodeId()) ? ", nodeId=" + request.getNodeId() : "") + (StringUtils.hasText(request.getStructureType()) ? ", structureType=" + request.getStructureType() : ""));
        broadcastAgentLog(mindmap.getId(), agentLogs.get(agentLogs.size() - 1));
        String target = request.getTargetType();
        if (!agentOps.isEmpty()) {
            for (Map<String, Object> op : agentOps) {
                String type = String.valueOf(op.getOrDefault("type", ""));
                if (!StringUtils.hasText(type)) continue;
                if (type.equals("delete_node") || type.equals("delete_subtree")) {
                    String lbl = String.valueOf(op.getOrDefault("nodeLabel", ""));
                    String id = findNodeIdByLabel(mindmap, lbl);
                    if (StringUtils.hasText(id)) {
                        Set<String> toRemove = new HashSet<>();
                        toRemove.add(id);
                        if (type.equals("delete_subtree")) {
                            Map<String, List<String>> children = new HashMap<>();
                            for (Map<String, Object> e : mindmap.getEdges()) {
                                String sId = String.valueOf(e.get("source"));
                                String tId = String.valueOf(e.get("target"));
                                children.computeIfAbsent(sId, k -> new ArrayList<>()).add(tId);
                            }
                            Deque<String> dq = new ArrayDeque<>();
                            dq.add(id);
                            while (!dq.isEmpty()) {
                                String cur = dq.pollFirst();
                                List<String> kids = children.getOrDefault(cur, Collections.emptyList());
                                for (String c : kids) if (toRemove.add(c)) dq.addLast(c);
                            }
                        }
                        mindmap.setNodes(mindmap.getNodes().stream().filter(n -> !toRemove.contains(String.valueOf(n.get("id")))).collect(Collectors.toList()));
                        mindmap.setEdges(mindmap.getEdges().stream().filter(e -> {
                            String sId = String.valueOf(e.get("source"));
                            String tId = String.valueOf(e.get("target"));
                            return !toRemove.contains(sId) && !toRemove.contains(tId);
                        }).collect(Collectors.toList()));
                        agentLogs.add((type.equals("delete_subtree") ? "Pruned subtree of " : "Deleted node ") + lbl);
                        broadcastAgentLog(mindmap.getId(), agentLogs.get(agentLogs.size() - 1));
                    }
                } else if (type.equals("update_node")) {
                    String lbl = String.valueOf(op.getOrDefault("nodeLabel", ""));
                    String newLabel = String.valueOf(op.getOrDefault("newLabel", ""));
                    String id = findNodeIdByLabel(mindmap, lbl);
                    if (StringUtils.hasText(id) && StringUtils.hasText(newLabel)) {
                        for (Map<String, Object> n : mindmap.getNodes()) {
                            if (id.equals(String.valueOf(n.get("id")))) {
                                Map<String, Object> data = (Map<String, Object>) n.getOrDefault("data", new HashMap<>());
                                data.put("label", newLabel);
                                n.put("data", data);
                                agentLogs.add("Updated node label: " + lbl + " → " + newLabel);
                                broadcastAgentLog(mindmap.getId(), agentLogs.get(agentLogs.size() - 1));
                                break;
                            }
                        }
                    }
                }
            }
        }

        if ("node".equalsIgnoreCase(target)) {
            if (!StringUtils.hasText(request.getNodeId())) {
                throw new InvalidMindmapDataException("nodeId", "nodeId bắt buộc khi tối ưu node");
            }
            // locate node
            Map<String, Object> node = mindmap.getNodes().stream()
                    .filter(n -> request.getNodeId().equals(String.valueOf(n.get("id"))))
                    .findFirst()
                    .orElseThrow(() -> new MindmapNotFoundException("Node không tồn tại trong mindmap", userId));

            boolean wantsDelete = false;
            boolean wantsPruneSubtree = false;
            if (request.getHints() != null) {
                for (String h : request.getHints()) {
                    String t = h == null ? "" : h.toLowerCase();
                    if (t.matches(".*(xóa|xoá|remove|delete|bỏ|loại bỏ|drop).*")) wantsDelete = true;
                    if (t.matches(".*(nhánh|branch|subtree|con|hậu duệ).*")) wantsPruneSubtree = true;
                }
            }

            if (wantsDelete) {
                String nodeId = request.getNodeId();
                Set<String> toRemove = new HashSet<>();
                toRemove.add(nodeId);
                if (wantsPruneSubtree) {
                    // Collect descendants via edges source->target
                    Map<String, List<String>> children = new HashMap<>();
                    for (Map<String, Object> e : mindmap.getEdges()) {
                        String s = String.valueOf(e.get("source"));
                        String t = String.valueOf(e.get("target"));
                        children.computeIfAbsent(s, k -> new ArrayList<>()).add(t);
                    }
                    Deque<String> dq = new ArrayDeque<>();
                    dq.add(nodeId);
                    while (!dq.isEmpty()) {
                        String cur = dq.pollFirst();
                        List<String> kids = children.getOrDefault(cur, Collections.emptyList());
                        for (String c : kids) {
                            if (toRemove.add(c)) dq.addLast(c);
                        }
                    }
                }
                List<Map<String, Object>> nextNodes = mindmap.getNodes().stream()
                        .filter(n -> !toRemove.contains(String.valueOf(n.get("id"))))
                        .collect(Collectors.toList());
                List<Map<String, Object>> nextEdges = mindmap.getEdges().stream()
                        .filter(e -> {
                            String s = String.valueOf(e.get("source"));
                            String t = String.valueOf(e.get("target"));
                            return !toRemove.contains(s) && !toRemove.contains(t);
                        })
                        .collect(Collectors.toList());

                UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                        .nodes(nextNodes)
                        .edges(nextEdges)
                        .build();
                MindmapResponse updated = mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);
                agentLogs.add("Pruned node " + nodeId + (wantsPruneSubtree ? " with subtree" : ""));
                appendAgentLogs(updated, agentLogs);
                broadcastAgentLog(mindmap.getId(), agentLogs.get(agentLogs.size() - 1));
                return updated;
            }

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
            // ensureLabelLength(newLabel);
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
            MindmapResponse updated = mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);
            agentLogs.add("Edited label of node " + request.getNodeId());
            appendAgentLogs(updated, agentLogs);
            broadcastAgentLog(mindmap.getId(), agentLogs.get(agentLogs.size() - 1));
            return updated;
        } else if ("description".equalsIgnoreCase(target)) {
            String currentDesc = mindmap.getDescription();
            String mapTitle = mindmap.getTitle();
            Map<String, Object> payload = buildGeminiPayloadForOptimizeDescription(mapTitle, currentDesc, lang, request.getHints(), "normal");
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
            MindmapResponse updated = mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);
            agentLogs.add("Updated description");
            appendAgentLogs(updated, agentLogs);
            broadcastAgentLog(mindmap.getId(), agentLogs.get(agentLogs.size() - 1));
            return updated;
        } else if ("structure".equalsIgnoreCase(target)) {
            boolean isReplace = agentPlan != null && "replace".equalsIgnoreCase(String.valueOf(agentPlan.get("action")));
            List<String> pruneLabels = agentPlan != null && agentPlan.get("pruneLabels") instanceof List<?> pl ? pl.stream().map(String::valueOf).collect(Collectors.toList()) : Collections.emptyList();
            if (!pruneLabels.isEmpty()) {
                Set<String> toRemove = new HashSet<>();
                for (String lbl : pruneLabels) {
                    String id = findNodeIdByLabel(mindmap, lbl);
                    if (StringUtils.hasText(id)) toRemove.add(id);
                }
                if (!toRemove.isEmpty()) {
                    List<Map<String, Object>> nextNodes0 = mindmap.getNodes().stream()
                            .filter(n -> !toRemove.contains(String.valueOf(n.get("id"))))
                            .collect(Collectors.toList());
                    List<Map<String, Object>> nextEdges0 = mindmap.getEdges().stream()
                            .filter(e -> {
                                String s = String.valueOf(e.get("source"));
                                String t = String.valueOf(e.get("target"));
                                return !toRemove.contains(s) && !toRemove.contains(t);
                            })
                            .collect(Collectors.toList());
                    mindmap.setNodes(nextNodes0);
                    mindmap.setEdges(nextEdges0);
                    agentLogs.add("Pruned labels: " + String.join(", ", pruneLabels));
                    broadcastAgentLog(mindmap.getId(), agentLogs.get(agentLogs.size() - 1));
                }
            }
            String requestedNodeId = request.getNodeId();
            String anchorNodeId = requestedNodeId;
            Map<String, Object> anchorNode = null;
            if (StringUtils.hasText(requestedNodeId)) {
                final String searchId = requestedNodeId;
                anchorNode = mindmap.getNodes().stream()
                        .filter(n -> searchId.equals(String.valueOf(n.get("id"))))
                        .findFirst()
                        .orElse(null);
            }
            if (anchorNode == null) {
                Set<String> targets = mindmap.getEdges().stream()
                        .map(e -> String.valueOf(e.get("target")))
                        .collect(Collectors.toSet());
                Optional<Map<String, Object>> rootNodeOpt = mindmap.getNodes().stream()
                        .filter(n -> !targets.contains(String.valueOf(n.get("id"))))
                        .findFirst();
                anchorNode = rootNodeOpt.orElseThrow(() -> new InvalidMindmapDataException("root", "Không tìm thấy node gốc"));
                anchorNodeId = String.valueOf(anchorNode.get("id"));
            }

            String anchorLabel = extractLabel(anchorNode);
            List<String> existingChildren = findChildrenLabels(mindmap, anchorNodeId);
            String mode = request.getMode() != null ? request.getMode() : "normal";
            int levels = request.getLevels() != null ? request.getLevels() : 2;
            int defaultFirst = "normal".equalsIgnoreCase(mode) ? 4 : 5;
            int firstLevelCount = request.getFirstLevelCount() != null ? request.getFirstLevelCount() : defaultFirst;
            int minFirst = "normal".equalsIgnoreCase(mode) ? 3 : 4;
            int maxFirst = "normal".equalsIgnoreCase(mode) ? 5 : 6;
            firstLevelCount = Math.max(minFirst, Math.min(maxFirst, firstLevelCount));

            String structureType = request.getStructureType() != null ? request.getStructureType() : "mindmap";
            String json;
            List<Map<String, Object>> rfNodes = new ArrayList<>();
            List<Map<String, Object>> rfEdges = new ArrayList<>();
            Map<String, String> idMap = new HashMap<>();
            Map<String, String> parentByTempId = new HashMap<>();
            if (isReplace) {
                String topic = (request.getHints() != null && !request.getHints().isEmpty()) ? request.getHints().get(0) : anchorLabel;
                Map<String, Object> payloadGen = buildGeminiPayloadForGenerate(topic, levels, firstLevelCount, lang, null, mode, minFirst, maxFirst);
                json = callGemini(payloadGen);
            } else {
                Map<String, Object> payload = buildGeminiPayloadForExpandStructure(anchorLabel, existingChildren, lang, request.getHints(), mode, minFirst, maxFirst, firstLevelCount, levels, structureType);
                json = callGemini(payload);
            }
            JsonNode root = parseJson(json);
            JsonNode nodesNode = root.get("nodes");
            if (nodesNode == null || !nodesNode.isArray()) {
                throw new InvalidMindmapDataException("nodes", "Phải là mảng node hợp lệ");
            }

            for (JsonNode n : nodesNode) {
                String tempId = textOrNull(n.get("id"));
                String label = textOrNull(n.get("label"));
                String parentTempId = textOrNull(n.get("parentId"));
                if (!StringUtils.hasText(tempId) || !StringUtils.hasText(label)) {
                    throw new InvalidMindmapDataException("node", "Thiếu id hoặc label");
                }
                parentByTempId.put(tempId, parentTempId);
            }

            Map<String, Integer> levelIndex = new HashMap<>();
            Map<String, Integer> nodeLevel = new HashMap<>();
            // compute levels
            for (JsonNode n : nodesNode) {
                String tempId = textOrNull(n.get("id"));
                String parentTempId = parentByTempId.get(tempId);
                int lvl = (parentTempId == null || parentTempId.isBlank()) ? 1 : (nodeLevel.getOrDefault(parentTempId, 1) + 1);
                nodeLevel.put(tempId, lvl);
                levelIndex.put(tempId, levelIndex.getOrDefault(parentTempId == null ? "__root__" : parentTempId, 0));
                levelIndex.put(parentTempId == null ? "__root__" : parentTempId, levelIndex.getOrDefault(parentTempId == null ? "__root__" : parentTempId, 0) + 1);
            }

            double anchorX = 0.0, anchorY = 0.0;
            Object posObj = anchorNode.get("position");
            if (posObj instanceof Map<?,?> m) {
                Object px = m.get("x");
                Object py = m.get("y");
                try {
                    anchorX = px != null ? Double.parseDouble(String.valueOf(px)) : 0.0;
                    anchorY = py != null ? Double.parseDouble(String.valueOf(py)) : 0.0;
                } catch (Exception ignored) { }
            }

            int topLevelCount = (int) parentByTempId.entrySet().stream().filter(e -> e.getValue() == null || e.getValue().isBlank()).count();
            double baseRadius = 900.0;
            double angleStep = topLevelCount > 0 ? (2 * Math.PI / topLevelCount) : (Math.PI / 3);

            Random rand = new Random();

            for (JsonNode n : nodesNode) {
                String tempId = textOrNull(n.get("id"));
                String label = textOrNull(n.get("label"));
                String newId = newNodeId();
                idMap.put(tempId, newId);
                Map<String, Object> data = new HashMap<>();
                data.put("label", label);
                // simple diversity
                String[] shapes = new String[]{"rectangle", "circle", "diamond", "hexagon", "ellipse", "roundedRectangle"};
                data.put("shape", shapes[rand.nextInt(shapes.length)]);
                String[] colors = new String[]{"#3b82f6", "#ef4444", "#10b981", "#f59e0b", "#8b5cf6"};
                data.put("color", colors[rand.nextInt(colors.length)]);
                Map<String, Object> position = new HashMap<>();
                String parentTempId = parentByTempId.get(tempId);
                int lvl = nodeLevel.getOrDefault(tempId, 1);
                if (parentTempId == null || parentTempId.isBlank()) {
                    int idx = levelIndex.getOrDefault("__root__", 0);
                    // compute index among top-level by scanning up to tempId
                    idx = (int) parentByTempId.entrySet().stream().filter(e -> e.getValue() == null || e.getValue().isBlank()).map(Map.Entry::getKey).collect(Collectors.toList()).indexOf(tempId);
                    if (idx < 0) idx = 0;
                    double angle = idx * angleStep;
                    if ("timeline".equalsIgnoreCase(structureType)) {
                        position.put("x", anchorX + (idx + 1) * 600);
                        position.put("y", anchorY);
                    } else if ("org".equalsIgnoreCase(structureType) || "tree".equalsIgnoreCase(structureType)) {
                        position.put("x", anchorX);
                        position.put("y", anchorY + (idx + 1) * 420);
                    } else {
                        position.put("x", anchorX + baseRadius * Math.cos(angle));
                        position.put("y", anchorY + baseRadius * Math.sin(angle));
                    }
                } else {
                    String parentNewId = idMap.get(parentTempId);
                    // default relative layout around parent
                    double px = anchorX, py = anchorY;
                    // try get parent rfNode we added
                    Optional<Map<String, Object>> parentNodeOpt = rfNodes.stream().filter(nn -> String.valueOf(nn.get("id")).equals(parentNewId)).findFirst();
                    if (parentNodeOpt.isPresent()) {
                        Object ppos = parentNodeOpt.get().get("position");
                        if (ppos instanceof Map<?,?> pm) {
                            Object pxo = pm.get("x");
                            Object pyo = pm.get("y");
                            try {
                                px = pxo != null ? Double.parseDouble(String.valueOf(pxo)) : anchorX;
                                py = pyo != null ? Double.parseDouble(String.valueOf(pyo)) : anchorY;
                            } catch (Exception ignored) { }
                        }
                    }
                    int childIdx = (int) parentByTempId.entrySet().stream().filter(e -> Objects.equals(e.getValue(), parentTempId)).map(Map.Entry::getKey).collect(Collectors.toList()).indexOf(tempId);
                    if (childIdx < 0) childIdx = 0;
                    double dx = 360 + childIdx * 180;
                    double dy = ((childIdx % 2 == 0) ? 1 : -1) * (260 + (lvl - 2) * 120);
                    if ("timeline".equalsIgnoreCase(structureType)) {
                        position.put("x", px + dx);
                        position.put("y", py + ((childIdx % 2 == 0) ? 240 : -240));
                    } else if ("org".equalsIgnoreCase(structureType) || "tree".equalsIgnoreCase(structureType)) {
                        position.put("x", px + dx);
                        position.put("y", py + ((childIdx % 2 == 0) ? 0 : 240));
                    } else if ("fishbone".equalsIgnoreCase(structureType)) {
                        position.put("x", px + dx);
                        position.put("y", py + ((childIdx % 2 == 0) ? -220 : 220));
                    } else {
                        position.put("x", px + dx * Math.cos(childIdx + 1));
                        position.put("y", py + dy);
                    }
                }
                Map<String, Object> rfNode = new HashMap<>();
                rfNode.put("id", newId);
                rfNode.put("type", "default");
                rfNode.put("data", data);
                rfNode.put("position", position);
                rfNodes.add(rfNode);
            }

            for (Map.Entry<String, String> e : parentByTempId.entrySet()) {
                String childTemp = e.getKey();
                String parentTemp = e.getValue();
                Map<String, Object> edge = new HashMap<>();
                edge.put("id", newEdgeId());
                if (parentTemp == null || parentTemp.isBlank()) {
                    if (isReplace) {
                        continue;
                    } else {
                        edge.put("source", anchorNodeId);
                        edge.put("target", idMap.get(childTemp));
                    }
                } else {
                    edge.put("source", idMap.get(parentTemp));
                    edge.put("target", idMap.get(childTemp));
                }
                edge.put("type", "smoothstep");
                edge.put("animated", true);
                rfEdges.add(edge);
            }

            List<Map<String, Object>> nextNodes;
            List<Map<String, Object>> nextEdges;
            if (isReplace) {
                nextNodes = new ArrayList<>(rfNodes);
                nextEdges = new ArrayList<>(rfEdges);
                agentLogs.add("Replace: rebuilt structure with " + rfNodes.size() + " nodes");
            } else {
                nextNodes = new ArrayList<>(mindmap.getNodes());
                nextNodes.addAll(rfNodes);
                nextEdges = new ArrayList<>(mindmap.getEdges());
                nextEdges.addAll(rfEdges);
                agentLogs.add("Expand: added " + rfNodes.size() + " nodes");
            }

            UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                    .nodes(nextNodes)
                    .edges(nextEdges)
                    .build();
            MindmapResponse updated = mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);
            appendAgentLogs(updated, agentLogs);
            for (String logMsg : agentLogs) broadcastAgentLog(mindmap.getId(), logMsg);
            return updated;
        } else {
            throw new InvalidMindmapDataException("targetType", "Giá trị không hợp lệ: node|description|structure");
        }
    }

    // private void ensureLabelLength(String label) {
    //     int words = Arrays.stream(label.trim().split("\\s+")).filter(s -> !s.isBlank()).toArray().length;
    //     if (words < 1 || words > 4) {
    //         throw new InvalidMindmapDataException("label", "Độ dài label phải 1–4 từ");
    //     }
    // }

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
            if (!org.springframework.util.StringUtils.hasText(geminiApiKey)) {
                log.error("Gemini API key missing");
                throw new com.riverflow.exception.AiUpstreamException(403, "Thiếu GEMINI_API_KEY trên server");
            }
            log.info("Gemini call model={}, hasKey={}, keyMask={}", model, true, maskKey(geminiApiKey));
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
            return ensureJson(s);
        } catch (com.riverflow.exception.AiUpstreamException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini call failed: {}", e.getMessage());
            throw new IllegalArgumentException("Không thể gọi AI vào lúc này, vui lòng thử lại.");
        }
    }


    private String maskKey(String k) {
        if (k == null || k.isBlank()) return "";
        int len = k.length();
        String start = k.substring(0, Math.min(4, len));
        String end = k.substring(Math.max(len - 4, 0));
        return start + "***" + end;
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
        String prompt = system + "\n\n" + user;
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", prompt))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);
        

        Map<String, Object> payload = new HashMap<>();
        /* removed system instruction field per Gemini v1 */ // dùng snake_case!
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig); 
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
        

        Map<String, Object> payload = new HashMap<>();
        /* removed system instruction field per Gemini v1 */ // dùng snake_case!
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
        

        Map<String, Object> payload = new HashMap<>();
        /* removed system instruction field per Gemini v1 */ // dùng snake_case!
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig); 
        return payload;
    }

    private Map<String, Object> buildGeminiPayloadForClassifyAction(String title, String currentDesc, List<String> nodeLabels, String lang, List<String> hints) {
        StringBuilder user = new StringBuilder();
        user.append("Bạn là hệ thống phân tích yêu cầu người dùng cho mindmap hiện tại.\n");
        user.append("Tiêu đề: ").append(title).append("\n");
        if (StringUtils.hasText(currentDesc)) user.append("Mô tả: ").append(currentDesc).append("\n");
        if (nodeLabels != null && !nodeLabels.isEmpty()) {
            user.append("Các node hiện có: ").append(String.join(", ", nodeLabels)).append("\n");
        }
        user.append("Yêu cầu: Hãy xuất một kế hoạch (plan) chi tiết, dùng đúng ID/label node hiện có. Plan là JSON: {\n  \"targetType\": \"structure|description|node\",\n  \"structureType\": \"mindmap|logic|brace|org|tree|timeline|fishbone\",\n  \"language\": \"vi|en\",\n  \"ops\": [\n    {\"type\": \"delete_node\", \"nodeLabel\": \"...\"},\n    {\"type\": \"delete_subtree\", \"nodeLabel\": \"...\"},\n    {\"type\": \"update_node\", \"nodeLabel\": \"...\", \"newLabel\": \"...\"},\n    {\"type\": \"add_node\", \"parentLabel\": \"...\", \"label\": \"...\"},\n    {\"type\": \"add_edge\", \"sourceLabel\": \"...\", \"targetLabel\": \"...\"}\n  ]\n}\n");
        user.append("Nếu người dùng nói rõ Thêm/Sửa/Xóa/Cập nhật thì plan phải nêu chính xác node/edge liên quan theo label/ID hiện có. Nếu không chắc, hãy chọn nhánh gốc (ROOT) làm parentLabel.\n");
        if (hints != null && !hints.isEmpty()) {
            user.append("Yêu cầu người dùng: ").append(String.join(" \n ", hints)).append("\n");
        }
        user.append("Nếu người dùng có ưu tiên về cấu trúc/ ngôn ngữ thì hãy ưu tiên theo lựa chọn đó.\n");
        user.append("Hãy quyết định hành động chính (targetType) và xuất \"ops\" chi tiết như mẫu trên, chỉ trả JSON hợp lệ.\n");
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString()))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2);
        Map<String, Object> payload = new HashMap<>();
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    private String findNodeIdByLabel(Mindmap mindmap, String nodeLabel) {
        if (!StringUtils.hasText(nodeLabel)) return null;
        String targetLower = nodeLabel.trim().toLowerCase();
        Optional<Map<String, Object>> exact = mindmap.getNodes().stream()
                .filter(n -> {
                    String l = extractLabel(n);
                    return StringUtils.hasText(l) && l.trim().equalsIgnoreCase(nodeLabel.trim());
                })
                .findFirst();
        if (exact.isPresent()) return String.valueOf(exact.get().get("id"));
        Optional<Map<String, Object>> contains = mindmap.getNodes().stream()
                .filter(n -> {
                    String l = extractLabel(n);
                    return StringUtils.hasText(l) && l.toLowerCase().contains(targetLower);
                })
                .findFirst();
        return contains.map(n -> String.valueOf(n.get("id"))).orElse(null);
    }

    private Map<String, Object> buildGeminiPayloadForExpandStructure(String anchorLabel, List<String> existingChildren, String lang, List<String> hints, String mode, int minFirst, int maxFirst, int firstLevelCount, int levels, String structureType) {
        StringBuilder user = new StringBuilder();
        user.append("Mở rộng mindmap cho nhánh: ");
        user.append(anchorLabel != null ? anchorLabel : "ROOT");
        user.append(". Ngôn ngữ: ").append(lang).append(". ");
        user.append("Độ sâu: ").append(levels).append(". Số node cấp 1: ").append(firstLevelCount).append(" (giới hạn ").append(minFirst).append("–").append(maxFirst).append(")").append(".\n");
        if (existingChildren != null && !existingChildren.isEmpty()) {
            user.append("Tránh trùng các nhánh hiện có: ").append(String.join(", ", existingChildren)).append("\n");
        }
        if (hints != null && !hints.isEmpty()) {
            user.append("Gợi ý: ").append(String.join(", ", hints)).append("\n");
        }
        user.append("Kiểu cấu trúc: ").append(structureType).append("\n");
        user.append("Trả JSON: { \"nodes\": [ {\"id\": \"n1\", \"label\": \"...\", \"parentId\": null }, {\"id\": \"n2\", \"label\": \"...\", \"parentId\": \"n1\" } ] }\n");
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString()))
        );
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);
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

    private Map<String, Object> extractAgentPlanFromHints(List<String> hints) {
        try {
            if (hints == null || hints.isEmpty()) return null;
            // Look for embedded plan
            for (String h : hints) {
                if (h != null && h.startsWith("AGENT_PLAN:")) {
                    String json = h.substring("AGENT_PLAN:".length()).trim();
                    Map<String, Object> plan = objectMapper.readValue(json, Map.class);
                    String action = String.valueOf(plan.getOrDefault("action", "expand"));
                    List<String> pruneLabels = plan.get("pruneIds") instanceof List<?> pl ? null : (plan.get("pruneLabels") instanceof List<?> p2 ? (List<String>) (List<?>) p2 : Collections.emptyList());
                    Map<String, Object> out = new HashMap<>();
                    out.put("action", action);
                    out.put("pruneLabels", pruneLabels != null ? pruneLabels : Collections.emptyList());
                    out.put("summary", plan.getOrDefault("summary", action));
                    return out;
                }
            }
            // Heuristic fallback
            String joined = String.join(" ", hints).toLowerCase();
            String action = joined.matches(".*(sửa lại|đổi|thay|chuyển sang|thành|replace|change to|update to).*") ? "replace"
                    : joined.matches(".*(xóa|xoá|remove|delete|loại bỏ|bỏ|drop).*") ? "prune"
                    : joined.matches(".*(sửa|chỉnh|update|edit|điều chỉnh).*") ? "edit" : "expand";
            Map<String, Object> out = new HashMap<>();
            out.put("action", action);
            out.put("pruneLabels", Collections.emptyList());
            out.put("summary", "action=" + action);
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private void appendAgentLogs(MindmapResponse resp, List<String> logs) {
        if (resp != null) {
            resp.setAiAgentLogs(logs);
        }
    }

    private void broadcastAgentLog(String mindmapId, String text) {
        try {
            if (!StringUtils.hasText(realtimeServerUrl)) return;
            Map<String, Object> body = new HashMap<>();
            body.put("mindmapId", mindmapId);
            body.put("event", "chat:message");
            Map<String, Object> data = new HashMap<>();
            data.put("message", text);
            data.put("from", "agent");
            body.put("data", data);
            WebClient.create(realtimeServerUrl)
                    .post()
                    .uri("/realtime/mindmap/event")
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe();
        } catch (Exception ignored) { }
    }
}
