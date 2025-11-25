package com.riverflow.service.mindmap.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Builds prompts for Gemini AI in different scenarios
 */
@Component
public class GeminiPromptBuilder {

        /**
         * Build prompt for classifying user action and planning operations
         */
        public Map<String, Object> buildClassifyActionPrompt(
                        String title,
                        String description,
                        List<Map<String, Object>> nodes,
                        List<Map<String, Object>> edges,
                        String language,
                        List<String> hints) {
                StringBuilder system = new StringBuilder();
                system.append("Bạn là trợ lý AI thông minh cho mindmap. Nhiệm vụ:\\n");
                system.append("1. FIRST: Giải thích bằng ngôn ngữ tự nhiên (").append(language)
                                .append(") những gì bạn sẽ làm\\n");
                system.append("2. THEN: Xuất kế hoạch JSON chi tiết\\n");
                system.append("Format: <natural language explanation>\\n\\n```json\\n<plan>\\n```\\n");

                StringBuilder user = new StringBuilder();
                user.append("Mindmap hiện tại:\\n");
                user.append("- Tiêu đề: ").append(title).append("\\n");
                if (StringUtils.hasText(description)) {
                        user.append("- Mô tả: ").append(description).append("\\n");
                }

                // Build comprehensive mindmap structure context
                if (nodes != null && !nodes.isEmpty()) {
                        user.append("- Tổng số nodes: ").append(nodes.size()).append("\\n");
                        user.append("\\nCấu trúc mindmap chi tiết:\\n");

                        // Build parent-child relationship map
                        Map<String, List<String>> childrenMap = new HashMap<>();
                        Set<String> allTargets = new HashSet<>();

                        if (edges != null) {
                                for (Map<String, Object> edge : edges) {
                                        String source = String.valueOf(edge.get("source"));
                                        String target = String.valueOf(edge.get("target"));
                                        childrenMap.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
                                        allTargets.add(target);
                                }
                        }

                        // Find root nodes (nodes with no incoming edges)
                        List<Map<String, Object>> rootNodes = nodes.stream()
                                        .filter(n -> !allTargets.contains(String.valueOf(n.get("id"))))
                                        .toList();

                        // Build node ID to label map
                        Map<String, String> idToLabel = new HashMap<>();
                        for (Map<String, Object> node : nodes) {
                                String id = String.valueOf(node.get("id"));
                                String label = extractNodeLabel(node);
                                idToLabel.put(id, label);
                        }

                        // Display hierarchical structure
                        for (Map<String, Object> rootNode : rootNodes) {
                                String rootId = String.valueOf(rootNode.get("id"));
                                String rootLabel = idToLabel.get(rootId);
                                user.append("ROOT: ").append(rootLabel).append(" [ID: ").append(rootId).append("]\\n");
                                buildNodeHierarchy(rootId, childrenMap, idToLabel, user, "  ", 0);
                        }

                        user.append("\\n");
                }
                user.append("\\n");

                if (hints != null && !hints.isEmpty()) {
                        user.append("Yêu cầu của người dùng:\\n");
                        user.append(String.join("\\n", hints)).append("\\n\\n");
                }

                user.append("Hãy:\\n");
                user.append("1. QUAN TRỌNG: Giải thích bằng ngôn ngữ tự nhiên (").append(language)
                                .append(") chi tiết, thân thiện những gì bạn sẽ làm (2-3 câu)\\n");
                user.append("2. Sau đó xuất JSON plan với format:\\n");
                user.append("```json\\n{\\n");
                user.append("  \\\"targetType\\\": \\\"structure|description|node\\\",\\n");
                user.append("  \\\"structureType\\\": \\\"mindmap|logic|brace|org|tree|timeline|fishbone\\\",\\n");
                user.append("  \\\"language\\\": \\\"vi|en\\\",\\n");
                user.append("  \\\"ops\\\": [\\n");
                user.append(
                                "    {\\\"type\\\": \\\"add_node\\\", \\\"parentLabel\\\": \\\"...\\\", \\\"label\\\": \\\"...\\\", \\\"description\\\": \\\"...\\\", \\\"color\\\": \\\"#...\\\", \\\"background\\\": \\\"#...\\\", \\\"icon\\\": \\\"emoji\\\"},\\n");
                user.append(
                                "    {\\\"type\\\": \\\"update_node\\\", \\\"nodeLabel\\\": \\\"...\\\", \\\"newLabel\\\": \\\"...\\\", \\\"newDescription\\\": \\\"...\\\", \\\"newColor\\\": \\\"#...\\\", \\\"newBackground\\\": \\\"#...\\\", \\\"newNodeType\\\": \\\"...\\\", \\\"newIcon\\\": \\\"...\\\"},\\n");
                user.append("    {\\\"type\\\": \\\"delete_node\\\", \\\"nodeLabel\\\": \\\"...\\\"},\\n");
                user.append("    {\\\"type\\\": \\\"delete_subtree\\\", \\\"nodeLabel\\\": \\\"...\\\"}  // Delete node and all children\\n");
                user.append("  ]\\n}\\n```\\n");
                user.append("CRITICAL: When adding nodes, ALWAYS provide a detailed 'description' in ").append(language)
                                .append(".\\n");

                Map<String, Object> systemInstruction = Map.of(
                                "parts", List.of(Map.of("text", system.toString())));
                Map<String, Object> userContent = Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", user.toString())));

                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("temperature", 0.3);

                Map<String, Object> payload = new HashMap<>();
                payload.put("systemInstruction", systemInstruction);
                payload.put("contents", List.of(userContent));
                payload.put("generationConfig", generationConfig);
                return payload;
        }

        /**
         * Build hierarchical display of nodes recursively
         */
        private void buildNodeHierarchy(String nodeId, Map<String, List<String>> childrenMap,
                        Map<String, String> idToLabel, StringBuilder output,
                        String indent, int depth) {
                // Limit depth to prevent overwhelming output
                if (depth > 5)
                        return;

                List<String> children = childrenMap.getOrDefault(nodeId, new ArrayList<>());
                for (String childId : children) {
                        String childLabel = idToLabel.get(childId);
                        output.append(indent).append("└─ ").append(childLabel)
                                        .append(" [ID: ").append(childId).append("]\\n");
                        buildNodeHierarchy(childId, childrenMap, idToLabel, output, indent + "  ", depth + 1);
                }
        }

        /**
         * Extract label from node data
         */
        private String extractNodeLabel(Map<String, Object> node) {
                Object data = node.get("data");
                if (data instanceof Map<?, ?> m) {
                        Object label = m.get("label");
                        return label != null ? String.valueOf(label) : String.valueOf(node.get("id"));
                }
                return String.valueOf(node.get("id"));
        }

        /**
         * Build payload for generating new mindmap
         */
        public Map<String, Object> buildGeneratePrompt(
                        String topic,
                        int levels,
                        int firstLevelCount,
                        String language,
                        List<String> tags,
                        String mode,
                        int minFirst,
                        int maxFirst,
                        String structureType) {
                StringBuilder system = new StringBuilder();
                system.append("You are a mindmap generator. JSON ONLY, no extra text. Rules:\\n");
                system.append("- LANGUAGE: ALL content (labels, descriptions) MUST be in ").append(language)
                                .append("\\n");
                system.append("- Structure type: ").append(structureType).append("\\n");
                system.append("- Node titles: clear, 1-4 words in ").append(language).append("\\n");
                system.append("- FirstLevel: ").append(firstLevelCount)
                                .append(" branches, MUST have parentId=null or empty\\n");
                system.append("- Max depth: ").append(levels).append(" levels\\n");
                system.append("- Diverse content, avoid repetition\\n");
                system.append("- Node properties: nodeType, colors, background, description, icons\\n");
                system.append("- Edge properties: type, animated, sourceHandle, targetHandle, markerEnd\\n");
                system.append(buildStructureGuidance(structureType));
                system.append("- Pure JSON output, no markdown```");

                StringBuilder user = new StringBuilder();
                user.append("Topic: ").append(topic).append("\\n");
                user.append("Language: ").append(language)
                                .append(" (REQUIRED - all content must be in this language)\\n");
                user.append("Structure: ").append(structureType).append("\\n");
                user.append("Levels: ").append(levels).append("\\n");
                user.append("First level count: ").append(firstLevelCount).append("\\n");
                if (tags != null && !tags.isEmpty()) {
                        user.append("Tags: ").append(String.join(", ", tags)).append("\\n");
                }
                user.append("Mode: ").append(mode).append("\\n\\n");
                user.append("Create detailed mindmap with rich properties:\\n");
                user.append("JSON format:\\n");
                user.append("{\\n");
                user.append("  \\\"nodes\\\": [\\n");
                user.append("    {\\n");
                user.append("      \\\"id\\\": \\\"unique-id\\\",\\n");
                user.append("      \\\"label\\\": \\\"Node title (concise, in ").append(language).append(")\\\",\\n");
                user.append("      \\\"description\\\": \\\"Detailed description (1-2 sentences, in ").append(language)
                                .append(")\\\",\\n");
                user.append("      \\\"parentId\\\": \\\"parent-id or null\\\",\\n");
                user.append("      \\\"nodeType\\\": \\\"default|input|output|decision|process\\\",\\n");
                user.append("      \\\"shape\\\": \\\"rectangle|circle|diamond|hexagon|ellipse|roundedRectangle\\\",\\n");
                user.append("      \\\"color\\\": \\\"#hex-color\\\",\\n");
                user.append("      \\\"background\\\": \\\"#hex-color\\\",\\n");
                user.append("      \\\"icon\\\": \\\"emoji\\\"\\n");
                user.append("    }\\n");
                user.append("  ],\\n");
                user.append("  \\\"edges\\\": [\\n");
                user.append("    {\\n");
                user.append("      \\\"source\\\": \\\"parent-id\\\",\\n");
                user.append("      \\\"target\\\": \\\"child-id\\\",\\n");
                user.append("      \\\"type\\\": \\\"smoothstep|step|straight|bezier (vary types)\\\",\\n");
                user.append("      \\\"animated\\\": true,\\n");
                user.append("      \\\"sourceHandle\\\": \\\"a|b|c|d (optional, varies)\\\",\\n");
                user.append("      \\\"targetHandle\\\": \\\"a|b|c|d (optional, varies)\\\",\\n");
                user.append("      \\\"markerEnd\\\": \\\"arrow|arrowclosed (optional)\\\"\\n");
                user.append("    }\\n");
                user.append("  ]\\n");
                user.append("}\\n\\n");
                user.append("CRITICAL RULES:\\n");
                user.append("1. LANGUAGE: ALL titles and descriptions MUST be in ").append(language).append(".\\n");
                user.append("2. SHAPES: Use diverse shapes (rectangle, circle, diamond, hexagon, ellipse, roundedRectangle).\\n");
                user.append("3. COLORS: Use vibrant, diverse colors for different branches.\\n");
                user.append("4. DESCRIPTIONS: Every node MUST have a detailed description in ").append(language)
                                .append(".\\n");
                user.append("5. EDGES: Vary edge types and handles for visual interest.\\n");

                Map<String, Object> systemInstruction = Map.of(
                                "parts", List.of(Map.of("text", system.toString())));
                Map<String, Object> userContent = Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", user.toString())));
                Map<String, Object> generationConfig = new HashMap<>();
                double temp = "max".equalsIgnoreCase(mode) ? 1.0
                                : ("thinking".equalsIgnoreCase(mode) ? 0.8 : 0.7);
                generationConfig.put("temperature", temp);
                generationConfig.put("maxOutputTokens", 8000);

                Map<String, Object> payload = new HashMap<>();
                payload.put("systemInstruction", systemInstruction);
                payload.put("contents", List.of(userContent));
                payload.put("generationConfig", generationConfig);
                return payload;
        }

        /**
         * Extract JSON from text that may contain markdown or extra text
         * Handles: ```json {...} ```, natural language + JSON, plain JSON
         */
        public String ensureJson(String text) {
                if (text == null || text.isBlank())
                        return null;

                String s = text.trim();

                // Try to find markdown JSON block first
                int jsonBlockStart = s.indexOf("```json");
                if (jsonBlockStart >= 0) {
                        int jsonStart = s.indexOf('{', jsonBlockStart);
                        int jsonBlockEnd = s.indexOf("```", jsonBlockStart + 7);
                        if (jsonStart >= 0 && jsonBlockEnd > jsonStart) {
                                String jsonContent = s.substring(jsonStart, jsonBlockEnd).trim();
                                int jsonEnd = jsonContent.lastIndexOf('}');
                                if (jsonEnd >= 0) {
                                        return jsonContent.substring(0, jsonEnd + 1).trim();
                                }
                        }
                }

                // Try to find plain JSON block
                int start = s.indexOf('{');
                int end = s.lastIndexOf('}');
                if (start >= 0 && end > start) {
                        return s.substring(start, end + 1).trim();
                }

                return s;
        }

        /**
         * Build structure-specific guidance for AI
         */
        private String buildStructureGuidance(String structureType) {
                if (structureType == null)
                        structureType = "mindmap";

                return switch (structureType.toLowerCase()) {
                        case "mindmap" -> "- Style: Colorful nodes, different sizes, radial layout\\n";
                        case "logic" -> "- Style: Rectangular boxes, use decision/process types\\n";
                        case "brace" -> "- Style: Grouped items, hierarchical structure\\n";
                        case "org" -> "- Style: Org chart roles, hierarchical positions\\n";
                        case "tree" -> "- Style: Tree branches, parent-child relationships\\n";
                        case "timeline" -> "- Style: Sequential events, chronological order\\n";
                        case "fishbone" -> "- Style: Cause-effect branches, problem analysis\\n";
                        default -> "- Style: Clear hierarchy and structure\\n";
                };
        }
}
