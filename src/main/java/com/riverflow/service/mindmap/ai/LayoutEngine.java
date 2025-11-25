package com.riverflow.service.mindmap.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Layout engine for auto-positioning mindmap nodes based on structure type
 */
@Component
@Slf4j
public class LayoutEngine {

    private static final int NODE_WIDTH = 200;
    private static final int NODE_HEIGHT = 80;
    private static final int HORIZONTAL_SPACING = 100;
    private static final int VERTICAL_SPACING = 120;
    private static final int LEVEL_SPACING = 250;

    /**
     * Apply layout to nodes based on structure type
     */
    public void applyLayout(String structureType, List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        if (nodes == null || nodes.isEmpty()) {
            log.warn("[Layout] No nodes to layout");
            return;
        }

        log.info("[Layout] Applying {} layout to {} nodes", structureType, nodes.size());

        switch (structureType.toLowerCase()) {
            case "mindmap" -> layoutMindmapStyle(nodes, edges);
            case "logic" -> layoutLogicChart(nodes, edges);
            case "brace" -> layoutBraceMap(nodes, edges);
            case "org" -> layoutOrgChart(nodes, edges);
            case "tree" -> layoutTreeChart(nodes, edges);
            case "timeline" -> layoutTimeline(nodes, edges);
            case "fishbone" -> layoutFishbone(nodes, edges);
            default -> layoutMindmapStyle(nodes, edges);
        }

        log.info("[Layout] Layout applied successfully");
    }

    /**
     * Mindmap style: Radial layout around center
     */
    private void layoutMindmapStyle(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, Integer> levels = buildLevelMap(nodes, edges);
        Map<Integer, List<Map<String, Object>>> nodesByLevel = groupNodesByLevel(nodes, levels);

        // Find root
        String rootId = findRootNode(nodes, edges);
        if (rootId != null) {
            Map<String, Object> root = findNodeById(nodes, rootId);
            if (root != null) {
                setPosition(root, 500, 400); // Center position
            }
        }

        // Layout level 1 nodes in circle around root
        List<Map<String, Object>> level1 = nodesByLevel.getOrDefault(1, new ArrayList<>());
        int numLevel1 = level1.size();
        double angleStep = 2 * Math.PI / Math.max(numLevel1, 1);
        int radius = 300;

        for (int i = 0; i < level1.size(); i++) {
            double angle = i * angleStep - Math.PI / 2; // Start from top
            int x = (int) (500 + radius * Math.cos(angle));
            int y = (int) (400 + radius * Math.sin(angle));
            setPosition(level1.get(i), x, y);
        }

        // Layout deeper levels
        for (int level = 2; level <= 5; level++) {
            List<Map<String, Object>> levelNodes = nodesByLevel.getOrDefault(level, new ArrayList<>());
            layoutChildrenAroundParents(levelNodes, nodes, edges, 200);
        }
    }

    /**
     * Logic chart: Flowchart-style top-down layout
     */
    private void layoutLogicChart(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, Integer> levels = buildLevelMap(nodes, edges);
        Map<Integer, List<Map<String, Object>>> nodesByLevel = groupNodesByLevel(nodes, levels);

        int startY = 100;
        int maxLevel = nodesByLevel.keySet().stream().max(Integer::compareTo).orElse(0);

        for (int level = 0; level <= maxLevel; level++) {
            List<Map<String, Object>> levelNodes = nodesByLevel.getOrDefault(level, new ArrayList<>());
            int numNodes = levelNodes.size();
            int totalWidth = numNodes * NODE_WIDTH + (numNodes - 1) * HORIZONTAL_SPACING;
            int startX = Math.max(100, (1200 - totalWidth) / 2);

            for (int i = 0; i < levelNodes.size(); i++) {
                int x = startX + i * (NODE_WIDTH + HORIZONTAL_SPACING);
                int y = startY + level * LEVEL_SPACING;
                setPosition(levelNodes.get(i), x, y);
            }
        }
    }

    /**
     * Brace map: Grouped hierarchical layout
     */
    private void layoutBraceMap(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, Integer> levels = buildLevelMap(nodes, edges);
        Map<Integer, List<Map<String, Object>>> nodesByLevel = groupNodesByLevel(nodes, levels);

        int startX = 100;
        int currentY = 100;

        // Root on left
        String rootId = findRootNode(nodes, edges);
        if (rootId != null) {
            Map<String, Object> root = findNodeById(nodes, rootId);
            if (root != null) {
                setPosition(root, startX, currentY + 200);
            }
        }

        // Each level further right
        for (int level = 1; level <= 5; level++) {
            List<Map<String, Object>> levelNodes = nodesByLevel.getOrDefault(level, new ArrayList<>());
            int x = startX + level * LEVEL_SPACING;
            int y = currentY;

            for (Map<String, Object> node : levelNodes) {
                setPosition(node, x, y);
                y += NODE_HEIGHT + VERTICAL_SPACING;
            }
        }
    }

    /**
     * Org chart: Hierarchical top-down layout
     */
    private void layoutOrgChart(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, Integer> levels = buildLevelMap(nodes, edges);
        Map<Integer, List<Map<String, Object>>> nodesByLevel = groupNodesByLevel(nodes, levels);

        int startY = 100;
        int maxLevel = nodesByLevel.keySet().stream().max(Integer::compareTo).orElse(0);

        for (int level = 0; level <= maxLevel; level++) {
            List<Map<String, Object>> levelNodes = nodesByLevel.getOrDefault(level, new ArrayList<>());
            int numNodes = levelNodes.size();
            int totalWidth = numNodes * NODE_WIDTH + (numNodes - 1) * HORIZONTAL_SPACING;
            int startX = Math.max(100, (1400 - totalWidth) / 2);

            for (int i = 0; i < levelNodes.size(); i++) {
                int x = startX + i * (NODE_WIDTH + HORIZONTAL_SPACING);
                int y = startY + level * (LEVEL_SPACING - 50);
                setPosition(levelNodes.get(i), x, y);
            }
        }
    }

    /**
     * Tree chart: Tree structure layout
     */
    private void layoutTreeChart(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        // Similar to org chart but with wider spacing
        layoutOrgChart(nodes, edges);
    }

    /**
     * Timeline: Horizontal or vertical linear layout
     */
    private void layoutTimeline(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        int startX = 150;
        int startY = 400;
        int spacing = 250;

        for (int i = 0; i < nodes.size(); i++) {
            int x = startX + i * spacing;
            setPosition(nodes.get(i), x, startY);
        }
    }

    /**
     * Fishbone: Fishbone diagram layout
     */
    private void layoutFishbone(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        if (nodes.isEmpty())
            return;

        // Main spine horizontal
        String rootId = findRootNode(nodes, edges);
        Map<String, Object> root = findNodeById(nodes, rootId);

        int spineY = 400;
        int spineStartX = 200;
        int spineEndX = 1000;

        if (root != null) {
            setPosition(root, spineEndX, spineY);
        }

        // Get children of root
        List<String> rootChildren = findChildren(rootId, edges);

        // Alternate branches above and below spine
        for (int i = 0; i < rootChildren.size(); i++) {
            Map<String, Object> child = findNodeById(nodes, rootChildren.get(i));
            if (child == null)
                continue;

            boolean isTop = i % 2 == 0;
            int branchX = spineStartX + (i / 2) * 200;
            int branchY = isTop ? spineY - 150 : spineY + 150;

            setPosition(child, branchX, branchY);

            // Layout sub-items
            List<String> subItems = findChildren(rootChildren.get(i), edges);
            for (int j = 0; j < subItems.size(); j++) {
                Map<String, Object> subItem = findNodeById(nodes, subItems.get(j));
                if (subItem != null) {
                    int subY = isTop ? branchY - 80 * (j + 1) : branchY + 80 * (j + 1);
                    setPosition(subItem, branchX - 50, subY);
                }
            }
        }
    }

    // === Helper Methods ===

    private Map<String, Integer> buildLevelMap(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, Integer> levels = new HashMap<>();
        Set<String> targets = edges.stream()
                .map(e -> String.valueOf(e.get("target")))
                .collect(Collectors.toSet());

        // Find roots (nodes with no incoming edges)
        List<String> roots = nodes.stream()
                .map(n -> String.valueOf(n.get("id")))
                .filter(id -> !targets.contains(id))
                .toList();

        // BFS to assign levels
        Map<String, List<String>> childrenMap = new HashMap<>();
        for (Map<String, Object> edge : edges) {
            String source = String.valueOf(edge.get("source"));
            String target = String.valueOf(edge.get("target"));
            childrenMap.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
        }

        Queue<String> queue = new LinkedList<>();
        for (String root : roots) {
            levels.put(root, 0);
            queue.offer(root);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentLevel = levels.get(current);
            List<String> children = childrenMap.getOrDefault(current, new ArrayList<>());

            for (String child : children) {
                if (!levels.containsKey(child)) {
                    levels.put(child, currentLevel + 1);
                    queue.offer(child);
                }
            }
        }

        return levels;
    }

    private Map<Integer, List<Map<String, Object>>> groupNodesByLevel(
            List<Map<String, Object>> nodes, Map<String, Integer> levels) {
        Map<Integer, List<Map<String, Object>>> result = new HashMap<>();

        for (Map<String, Object> node : nodes) {
            String id = String.valueOf(node.get("id"));
            int level = levels.getOrDefault(id, 0);
            result.computeIfAbsent(level, k -> new ArrayList<>()).add(node);
        }

        return result;
    }

    private String findRootNode(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Set<String> targets = edges.stream()
                .map(e -> String.valueOf(e.get("target")))
                .collect(Collectors.toSet());

        return nodes.stream()
                .map(n -> String.valueOf(n.get("id")))
                .filter(id -> !targets.contains(id))
                .findFirst()
                .orElse(nodes.isEmpty() ? null : String.valueOf(nodes.get(0).get("id")));
    }

    private Map<String, Object> findNodeById(List<Map<String, Object>> nodes, String id) {
        if (id == null)
            return null;
        return nodes.stream()
                .filter(n -> id.equals(String.valueOf(n.get("id"))))
                .findFirst()
                .orElse(null);
    }

    private List<String> findChildren(String parentId, List<Map<String, Object>> edges) {
        if (parentId == null)
            return new ArrayList<>();
        return edges.stream()
                .filter(e -> parentId.equals(String.valueOf(e.get("source"))))
                .map(e -> String.valueOf(e.get("target")))
                .toList();
    }

    private void setPosition(Map<String, Object> node, int x, int y) {
        if (node == null)
            return;
        Map<String, Object> position = new HashMap<>();
        position.put("x", x);
        position.put("y", y);
        node.put("position", position);
    }

    private void layoutChildrenAroundParents(List<Map<String, Object>> children,
            List<Map<String, Object>> allNodes, List<Map<String, Object>> edges, int radius) {

        // Group children by parent
        Map<String, List<Map<String, Object>>> childrenByParent = new HashMap<>();
        for (Map<String, Object> child : children) {
            String childId = String.valueOf(child.get("id"));
            String parentId = edges.stream()
                    .filter(e -> childId.equals(String.valueOf(e.get("target"))))
                    .map(e -> String.valueOf(e.get("source")))
                    .findFirst()
                    .orElse(null);

            if (parentId != null) {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(child);
            }
        }

        // Layout children for each parent
        for (Map.Entry<String, List<Map<String, Object>>> entry : childrenByParent.entrySet()) {
            String parentId = entry.getKey();
            List<Map<String, Object>> siblings = entry.getValue();
            Map<String, Object> parent = findNodeById(allNodes, parentId);

            if (parent != null) {
                Map<?, ?> parentPos = (Map<?, ?>) parent.get("position");
                if (parentPos != null) {
                    int parentX = ((Number) parentPos.get("x")).intValue();
                    int parentY = ((Number) parentPos.get("y")).intValue();

                    // Find grandparent to determine direction
                    String grandParentId = edges.stream()
                            .filter(e -> parentId.equals(String.valueOf(e.get("target"))))
                            .map(e -> String.valueOf(e.get("source")))
                            .findFirst()
                            .orElse(null);

                    double baseAngle = 0;
                    if (grandParentId != null) {
                        Map<String, Object> grandParent = findNodeById(allNodes, grandParentId);
                        if (grandParent != null) {
                            Map<?, ?> gpPos = (Map<?, ?>) grandParent.get("position");
                            int gpX = ((Number) gpPos.get("x")).intValue();
                            int gpY = ((Number) gpPos.get("y")).intValue();
                            baseAngle = Math.atan2(parentY - gpY, parentX - gpX);
                        }
                    } else {
                        // If root (or no grandparent), base angle depends on position relative to
                        // center
                        baseAngle = Math.atan2(parentY - 400, parentX - 500);
                    }

                    // Distribute children in a fan (e.g., 120 degrees arc)
                    int count = siblings.size();
                    double arc = Math.toRadians(120); // 120 degrees coverage
                    double startAngle = baseAngle - arc / 2;
                    double step = count > 1 ? arc / (count - 1) : 0;

                    if (count == 1) {
                        startAngle = baseAngle;
                    }

                    for (int i = 0; i < count; i++) {
                        double angle = startAngle + i * step;
                        int x = (int) (parentX + radius * Math.cos(angle));
                        int y = (int) (parentY + radius * Math.sin(angle));
                        setPosition(siblings.get(i), x, y);
                    }
                }
            }
        }
    }
}
