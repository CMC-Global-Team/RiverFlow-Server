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

        log.info("[Delete Node] Attempting to delete node '{}'", nodeLabel);

        if (!StringUtils.hasText(nodeId)) {
            log.warn("[Delete Node] Node not found: {}", nodeLabel);
            return "Node not found: " + nodeLabel;
        }

        int beforeNodes = mindmap.getNodes().size();
        int beforeEdges = mindmap.getEdges().size();

        log.info("[Delete Node] Found node to delete: id='{}', label='{}'", nodeId, nodeLabel);
        log.info("[Delete Node] Before deletion: nodes={}, edges={}", beforeNodes, beforeEdges);

        mindmap.setNodes(mindmap.getNodes().stream()
                .filter(n -> !nodeId.equals(String.valueOf(n.get("id"))))
                .collect(Collectors.toList()));

        mindmap.setEdges(mindmap.getEdges().stream()
                .filter(e -> !nodeId.equals(String.valueOf(e.get("source")))
                        && !nodeId.equals(String.valueOf(e.get("target"))))
                .collect(Collectors.toList()));

        int afterNodes = mindmap.getNodes().size();
        int afterEdges = mindmap.getEdges().size();

        log.info("[Delete Node] After deletion: nodes={}, edges={}", afterNodes, afterEdges);
        log.info("[Delete Node] Deleted {} nodes and {} edges", beforeNodes - afterNodes, beforeEdges - afterEdges);

        String result = "Deleted node: " + nodeLabel;
        log.info("[Delete Node] Success: {}", result);
        return result;
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
        String newDescription = String.valueOf(op.getOrDefault("newDescription", ""));
        String newNodeType = String.valueOf(op.getOrDefault("newNodeType", ""));
        String newColor = String.valueOf(op.getOrDefault("newColor", ""));
        String newBackground = String.valueOf(op.getOrDefault("newBackground", ""));
        String newIcon = String.valueOf(op.getOrDefault("newIcon", ""));

        String nodeId = findNodeIdByLabel(mindmap, oldLabel);

        if (!StringUtils.hasText(nodeId)) {
            return "Failed to update node: " + oldLabel + " not found";
        }

        boolean updated = false;
        for (Map<String, Object> node : mindmap.getNodes()) {
            if (nodeId.equals(String.valueOf(node.get("id")))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) node.getOrDefault("data", new HashMap<>());
                @SuppressWarnings("unchecked")
                Map<String, Object> style = (Map<String, Object>) node.getOrDefault("style", new HashMap<>());

                // Update label
                if (StringUtils.hasText(newLabel) && !"null".equals(newLabel)) {
                    data.put("label", newLabel);
                }

                // Update description
                if (StringUtils.hasText(newDescription) && !"null".equals(newDescription)) {
                    data.put("description", newDescription);
                }

                // Update icon
                if (StringUtils.hasText(newIcon) && !"null".equals(newIcon)) {
                    data.put("icon", newIcon);
                }

                // Update colors in style
                if (StringUtils.hasText(newColor) && !"null".equals(newColor)) {
                    style.put("color", newColor);
                }

                if (StringUtils.hasText(newBackground) && !"null".equals(newBackground)) {
                    style.put("background", newBackground);
                }

                // Update node type
                if (StringUtils.hasText(newNodeType) && !"null".equals(newNodeType)) {
                    node.put("type", newNodeType);
                }

                node.put("data", data);
                if (!style.isEmpty()) {
                    node.put("style", style);
                }

                updated = true;
                break;
            }
        }

        if (updated) {
            // Force MongoDB change detection by creating new ArrayList
            mindmap.setNodes(new ArrayList<>(mindmap.getNodes()));
            log.info("[Update Node] Forced change detection for mindmap nodes");

            List<String> changes = new ArrayList<>();
            if (StringUtils.hasText(newLabel) && !"null".equals(newLabel))
                changes.add("label→" + newLabel);
            if (StringUtils.hasText(newDescription) && !"null".equals(newDescription))
                changes.add("description");
            if (StringUtils.hasText(newColor) && !"null".equals(newColor))
                changes.add("color→" + newColor);
            if (StringUtils.hasText(newBackground) && !"null".equals(newBackground))
                changes.add("background→" + newBackground);
            if (StringUtils.hasText(newNodeType) && !"null".equals(newNodeType))
                changes.add("type→" + newNodeType);
            if (StringUtils.hasText(newIcon) && !"null".equals(newIcon))
                changes.add("icon→" + newIcon);

            return "Updated node '" + oldLabel + "': " + String.join(", ", changes);
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

        // Parse rich properties from AI operation
        String nodeType = String.valueOf(op.getOrDefault("nodeType", "default"));
        String color = String.valueOf(op.getOrDefault("color", ""));
        String background = String.valueOf(op.getOrDefault("background", ""));
        String icon = String.valueOf(op.getOrDefault("icon", ""));
        String description = String.valueOf(op.getOrDefault("description", ""));

        Map<String, Object> data = new HashMap<>();
        data.put("label", label);
        if (StringUtils.hasText(description) && !"null".equals(description)) {
            data.put("description", description);
        }
        if (StringUtils.hasText(icon) && !"null".equals(icon)) {
            data.put("icon", icon);
        }

        Map<String, Object> style = new HashMap<>();
        if (StringUtils.hasText(background) && !"null".equals(background)) {
            style.put("background", background);
        }
        if (StringUtils.hasText(color) && !"null".equals(color)) {
            style.put("color", color);
        }

        Map<String, Object> position = new HashMap<>();
        // Calculate position near parent
        if (StringUtils.hasText(parentId)) {
            Map<String, Object> parent = findNodeById(mindmap, parentId);
            if (parent != null && parent.get("position") instanceof Map<?, ?> parentPos) {
                int parentX = ((Number) parentPos.get("x")).intValue();
                int parentY = ((Number) parentPos.get("y")).intValue();
                // Offset new node from parent
                position.put("x", parentX + 200);
                position.put("y", parentY + 100);
            } else {
                position.put("x", 0);
                position.put("y", 0);
            }
        } else {
            position.put("x", 0);
            position.put("y", 0);
        }

        Map<String, Object> newNode = new HashMap<>();
        newNode.put("id", newId);
        newNode.put("type", StringUtils.hasText(nodeType) && !"null".equals(nodeType) ? nodeType : "default");
        newNode.put("data", data);
        if (!style.isEmpty()) {
            newNode.put("style", style);
        }
        newNode.put("position", position);

        mindmap.getNodes().add(newNode);
        // Force MongoDB change detection by creating new ArrayList
        mindmap.setNodes(new ArrayList<>(mindmap.getNodes()));
        log.info("[Add Node] Created new node: id='{}', label='{}', type='{}'", newId, label, nodeType);

        if (StringUtils.hasText(parentId)) {
            Map<String, Object> edge = new HashMap<>();
            edge.put("id", NEW_EDGE_PREFIX + UUID.randomUUID());
            edge.put("source", parentId);
            edge.put("target", newId);
            edge.put("type", "smoothstep");
            edge.put("animated", true);
            mindmap.getEdges().add(edge);
            // Force MongoDB change detection by creating new ArrayList
            mindmap.setEdges(new ArrayList<>(mindmap.getEdges()));
            log.info("[Add Node] Created edge from '{}' to '{}'", parentId, newId);
        }

        log.info("[Add Node] Forced change detection for mindmap nodes and edges");
        String result = "Added node: " + label + (StringUtils.hasText(parentLabel) ? " under " + parentLabel : "");
        log.info("[Add Node] Success: {}", result);
        return result;
    }

    private Map<String, Object> findNodeById(Mindmap mindmap, String id) {
        if (id == null)
            return null;
        return mindmap.getNodes().stream()
                .filter(n -> id.equals(String.valueOf(n.get("id"))))
                .findFirst()
                .orElse(null);
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
        // Force MongoDB change detection by creating new ArrayList
        mindmap.setEdges(new ArrayList<>(mindmap.getEdges()));
        log.info("[Add Edge] Forced change detection for mindmap edges");

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
