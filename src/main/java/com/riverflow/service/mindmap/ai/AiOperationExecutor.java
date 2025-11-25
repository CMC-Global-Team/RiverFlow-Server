package com.riverflow.service.mindmap.ai;

import com.riverflow.model.mindmap.Mindmap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Executes AI-determined operations on mindmaps.
 * Handles add/delete/update operations for nodes and edges based on Gemini AI
 * decisions.
 */
@Component
@Slf4j
public class AiOperationExecutor {

    private static final String NEW_NODE_PREFIX = "node-";
    private static final String NEW_EDGE_PREFIX = "edge-";

    /**
     * Execute a list of operations returned by AI
     */
    public List<String> executeOperations(List<Map<String, Object>> ops, Mindmap mindmap) {
        List<String> logs = new ArrayList<>();

        for (Map<String, Object> op : ops) {
            String type = String.valueOf(op.getOrDefault("type", ""));
            if (!StringUtils.hasText(type))
                continue;

            try {
                switch (type) {
                    case "delete_node" -> logs.add(deleteNode(op, mindmap));
                    case "delete_subtree" -> logs.add(deleteSubtree(op, mindmap));
                    case "update_node" -> logs.add(updateNode(op, mindmap));
                    case "add_node" -> logs.add(addNode(op, mindmap));
                    case "add_edge" -> logs.add(addEdge(op, mindmap));
                    default -> log.warn("Unknown operation type: {}", type);
                }
            } catch (Exception e) {
                log.error("Error executing operation {}: {}", type, e.getMessage());
            }
        }

        return logs;
    }

    private String deleteNode(Map<String, Object> op, Mindmap mindmap) {
        String nodeLabel = String.valueOf(op.getOrDefault("nodeLabel", ""));
        String nodeId = findNodeIdByLabel(mindmap, nodeLabel);

        if (!StringUtils.hasText(nodeId)) {
            return "Node not found: " + nodeLabel;
        }

        mindmap.setNodes(mindmap.getNodes().stream()
                .filter(n -> !nodeId.equals(String.valueOf(n.get("id"))))
                .collect(Collectors.toList()));

        mindmap.setEdges(mindmap.getEdges().stream()
                .filter(e -> !nodeId.equals(String.valueOf(e.get("source")))
                        && !nodeId.equals(String.valueOf(e.get("target"))))
                .collect(Collectors.toList()));

        return "Deleted node: " + nodeLabel;
    }

    private String deleteSubtree(Map<String, Object> op, Mindmap mindmap) {
        String nodeLabel = String.valueOf(op.getOrDefault("nodeLabel", ""));
        String rootId = findNodeIdByLabel(mindmap, nodeLabel);

        if (!StringUtils.hasText(rootId)) {
            return "Node not found: " + nodeLabel;
        }

        Set<String> toRemove = new HashSet<>();
        toRemove.add(rootId);

        // Build children map
        Map<String, List<String>> children = new HashMap<>();
        for (Map<String, Object> edge : mindmap.getEdges()) {
            String source = String.valueOf(edge.get("source"));
            String target = String.valueOf(edge.get("target"));
            children.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
        }

        // BFS to find all descendants
        Deque<String> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            String current = queue.pollFirst();
            List<String> kids = children.getOrDefault(current, Collections.emptyList());
            for (String kid : kids) {
                if (toRemove.add(kid)) {
                    queue.addLast(kid);
                }
            }
        }

        mindmap.setNodes(mindmap.getNodes().stream()
                .filter(n -> !toRemove.contains(String.valueOf(n.get("id"))))
                .collect(Collectors.toList()));

        mindmap.setEdges(mindmap.getEdges().stream()
                .filter(e -> !toRemove.contains(String.valueOf(e.get("source")))
                        && !toRemove.contains(String.valueOf(e.get("target"))))
                .collect(Collectors.toList()));

        return "Pruned subtree of: " + nodeLabel;
    }

    private String updateNode(Map<String, Object> op, Mindmap mindmap) {
        String oldLabel = String.valueOf(op.getOrDefault("nodeLabel", ""));
        String newLabel = String.valueOf(op.getOrDefault("newLabel", ""));
        String nodeId = findNodeIdByLabel(mindmap, oldLabel);

        if (!StringUtils.hasText(nodeId) || !StringUtils.hasText(newLabel)) {
            return "Failed to update node: " + oldLabel;
        }

        for (Map<String, Object> node : mindmap.getNodes()) {
            if (nodeId.equals(String.valueOf(node.get("id")))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) node.getOrDefault("data", new HashMap<>());
                data.put("label", newLabel);
                node.put("data", data);
                return "Updated node label: " + oldLabel + " → " + newLabel;
            }
        }

        return "Node not found: " + oldLabel;
    }

    private String addNode(Map<String, Object> op, Mindmap mindmap) {
        String parentLabel = String.valueOf(op.getOrDefault("parentLabel", ""));
        String label = String.valueOf(op.getOrDefault("label", ""));

        log.info("[Add Node] Attempting to add node '{}' under parent '{}'", label, parentLabel);

        if (!StringUtils.hasText(label)) {
            log.warn("[Add Node] Failed: empty label");
            return "Failed to add node: empty label";
        }

        String parentId = StringUtils.hasText(parentLabel)
                ? findNodeIdByLabel(mindmap, parentLabel)
                : findRootNodeId(mindmap);

        if (!StringUtils.hasText(parentId)) {
            log.warn("[Add Node] Parent '{}' not found, using root", parentLabel);
            parentId = findRootNodeId(mindmap);
        } else {
            log.info("[Add Node] Found parent: label='{}', id='{}'", parentLabel, parentId);
        }

        String newId = NEW_NODE_PREFIX + UUID.randomUUID();

        Map<String, Object> data = new HashMap<>();
        data.put("label", label);

        Map<String, Object> position = new HashMap<>();
        position.put("x", 0);
        position.put("y", 0);

        Map<String, Object> newNode = new HashMap<>();
        newNode.put("id", newId);
        newNode.put("type", "default");
        newNode.put("data", data);
        newNode.put("position", position);

        mindmap.getNodes().add(newNode);
        log.info("[Add Node] Created new node: id='{}', label='{}'", newId, label);

        if (StringUtils.hasText(parentId)) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", NEW_EDGE_PREFIX + UUID.randomUUID());
            edge.put("source", parentId);
            edge.put("target", newId);
            edge.put("type", "smoothstep");
            edge.put("animated", true);
            mindmap.getEdges().add(edge);
            log.info("[Add Node] Created edge from '{}' to '{}'", parentId, newId);
        }

        String result = "Added node: " + label + (StringUtils.hasText(parentLabel) ? " under " + parentLabel : "");
        log.info("[Add Node] Success: {}", result);
        return result;
    }

    private String addEdge(Map<String, Object> op, Mindmap mindmap) {
        String sourceLabel = String.valueOf(op.getOrDefault("sourceLabel", ""));
        String targetLabel = String.valueOf(op.getOrDefault("targetLabel", ""));

        String sourceId = findNodeIdByLabel(mindmap, sourceLabel);
        String targetId = findNodeIdByLabel(mindmap, targetLabel);

        if (!StringUtils.hasText(sourceId) || !StringUtils.hasText(targetId)) {
            return "Failed to add edge: nodes not found";
        }

        boolean exists = mindmap.getEdges().stream()
                .anyMatch(e -> Objects.equals(String.valueOf(e.get("source")), sourceId)
                        && Objects.equals(String.valueOf(e.get("target")), targetId));

        if (exists) {
            return "Edge already exists: " + sourceLabel + " → " + targetLabel;
        }

        Map<String, Object> edge = new HashMap<>();
        edge.put("id", NEW_EDGE_PREFIX + UUID.randomUUID());
        edge.put("source", sourceId);
        edge.put("target", targetId);
        edge.put("type", "smoothstep");
        edge.put("animated", true);
        mindmap.getEdges().add(edge);

        return "Added edge: " + sourceLabel + " → " + targetLabel;
    }

    private String findNodeIdByLabel(Mindmap mindmap, String nodeLabel) {
        if (!StringUtils.hasText(nodeLabel)) {
            log.warn("[Find Node] Empty node label provided");
            return null;
        }

        String targetLower = nodeLabel.trim().toLowerCase();

        // Log all available nodes for debugging
        log.info("[Find Node] Searching for '{}' among {} nodes:", nodeLabel, mindmap.getNodes().size());
        for (Map<String, Object> n : mindmap.getNodes()) {
            String label = extractLabel(n);
            String id = String.valueOf(n.get("id"));
            log.info("  - Node: id='{}', label='{}'", id, label);
        }

        // Exact match first
        Optional<Map<String, Object>> exact = mindmap.getNodes().stream()
                .filter(n -> {
                    String label = extractLabel(n);
                    return StringUtils.hasText(label) && label.trim().equalsIgnoreCase(nodeLabel.trim());
                })
                .findFirst();

        if (exact.isPresent()) {
            String foundId = String.valueOf(exact.get().get("id"));
            String foundLabel = extractLabel(exact.get());
            log.info("[Find Node] Exact match found: '{}' -> id='{}'", foundLabel, foundId);
            return foundId;
        }

        // Contains match
        Optional<Map<String, Object>> contains = mindmap.getNodes().stream()
                .filter(n -> {
                    String label = extractLabel(n);
                    return StringUtils.hasText(label) && label.toLowerCase().contains(targetLower);
                })
                .findFirst();

        if (contains.isPresent()) {
            String foundId = String.valueOf(contains.get().get("id"));
            String foundLabel = extractLabel(contains.get());
            log.info("[Find Node] Contains match found: '{}' contains '{}' -> id='{}'", foundLabel, nodeLabel, foundId);
            return foundId;
        }

        log.warn("[Find Node] No match found for '{}'", nodeLabel);
        return null;
    }

    private String findRootNodeId(Mindmap mindmap) {
        Set<String> targets = mindmap.getEdges().stream()
                .map(e -> String.valueOf(e.get("target")))
                .collect(Collectors.toSet());

        Optional<Map<String, Object>> root = mindmap.getNodes().stream()
                .filter(n -> !targets.contains(String.valueOf(n.get("id"))))
                .findFirst();

        return root.map(n -> String.valueOf(n.get("id")))
                .orElseGet(() -> mindmap.getNodes().isEmpty() ? null
                        : String.valueOf(mindmap.getNodes().get(0).get("id")));
    }

    private String extractLabel(Map<String, Object> node) {
        Object data = node.get("data");
        if (data instanceof Map<?, ?> m) {
            Object label = m.get("label");
            return label != null ? String.valueOf(label) : null;
        }
        return null;
    }
}
