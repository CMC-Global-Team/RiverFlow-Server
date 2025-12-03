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
                        List<String> hints,
                        String structureType) {
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

                        // Build node ID to Node map for detailed context
                        Map<String, Map<String, Object>> idToNode = new HashMap<>();
                        for (Map<String, Object> node : nodes) {
                                String id = String.valueOf(node.get("id"));
                                idToNode.put(id, node);
                        }

                        // Display hierarchical structure with properties
                        for (Map<String, Object> rootNode : rootNodes) {
                                String rootId = String.valueOf(rootNode.get("id"));
                                String rootLabel = extractNodeLabel(rootNode);
                                String props = extractNodeProperties(rootNode);
                                user.append("ROOT: ").append(rootLabel).append(" [ID: ").append(rootId).append("] ").append(props).append("\\n");
                                buildNodeHierarchy(rootId, childrenMap, idToNode, user, "  ", 0);
                        }

                        user.append("\\n");
                }
                user.append("\\n");

                // Add structure type requirement if specified
                if (StringUtils.hasText(structureType) && !"mindmap".equalsIgnoreCase(structureType)) {
                        user.append("STRUCTURE TYPE REQUIRED: ").append(structureType).append("\\n");
                        user.append("You MUST return this structureType in your JSON response.\\n\\n");
                }

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
         * Build prompt that asks Gemini to THINK and output OTMZ JSON only.
         */
        public Map<String, Object> buildThinkingOtmzPrompt(
                String topic,
                String language,
                String structureType,
                Integer levels,
                Integer firstLevelCount,
                List<String> tags,
                String mode) {

            String lang = StringUtils.hasText(language) ? language : "vi";
            String struct = StringUtils.hasText(structureType) ? structureType : "mindmap";
            int depth = levels != null ? levels : 2;
            int first = firstLevelCount != null ? firstLevelCount : 4;

            StringBuilder system = new StringBuilder();
            system.append("You analyze a topic and design a mindmap plan.\n");
            system.append("IMPORTANT: Respond in TWO parts:\n");
            system.append("1. FIRST: A friendly natural language explanation (2-3 sentences) in ").append(lang).append(" describing what you're creating\n");
            system.append("2. THEN: The complete OTMZ JSON on a new line\n");
            system.append("Format: <explanation>\\n\\n```json\\n<otmz>\\n```\n");
            system.append("Rules for JSON:\n");
            system.append("- All labels/descriptions MUST be in ").append(lang).append(".\n");
            system.append("- Structure type: ").append(struct).append(".\n");
            system.append("- Provide concise titles (1-4 words) and 1-2 sentence descriptions.\n");
            system.append("- Respect levels and firstLevelCount.\n");
            system.append(buildStructureGuidance(struct));

            StringBuilder user = new StringBuilder();
            user.append("Topic: ").append(topic).append("\n");
            user.append("Language: ").append(lang).append("\n");
            user.append("Structure: ").append(struct).append("\n");
            user.append("Levels: ").append(depth).append("\n");
            user.append("FirstLevelCount: ").append(first).append("\n");
            if (tags != null && !tags.isEmpty()) {
                user.append("Tags: ").append(String.join(", ", tags)).append("\n");
            }
            user.append("\nPlease:\n");
            user.append("1. First explain in ").append(lang).append(" what mindmap you're creating\n");
            user.append("2. Then provide the OTMZ JSON with all required fields\n");

            Map<String, Object> systemInstruction = Map.of(
                    "parts", List.of(Map.of("text", system.toString())));
            Map<String, Object> userContent = Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", user.toString())));

            Map<String, Object> generationConfig = new HashMap<>();
            double temp = "max".equalsIgnoreCase(mode) ? 1.0
                    : ("thinking".equalsIgnoreCase(mode) ? 0.8 : 0.7);
            generationConfig.put("temperature", temp);
            generationConfig.put("maxOutputTokens", 6000);

            Map<String, Object> payload = new HashMap<>();
            payload.put("systemInstruction", systemInstruction);
            payload.put("contents", List.of(userContent));
            payload.put("generationConfig", generationConfig);
            return payload;
        }

        /**
     * Build prompt that converts an OTMZ JSON into an Action List JSON.
     */
    public Map<String, Object> buildActionListPrompt(String otmzJson, String language) {
        String lang = StringUtils.hasText(language) ? language : "vi";

        StringBuilder system = new StringBuilder();
        system.append("You transform an OTMZ JSON into an execution Action List.\n");
        system.append("IMPORTANT: Respond in TWO parts:\n");
        system.append("1. FIRST: A brief friendly explanation (1-2 sentences) in ").append(lang).append(" of what actions you're generating\n");
        system.append("2. THEN: The complete Action List JSON on a new line\n");
        system.append("Format: <explanation>\\n\\n```json\\n<actions>\\n```\n");
        system.append("Rules for JSON:\n");
        system.append("- Allowed action types: add_node, update_node, delete_node, delete_subtree, set_title, set_structureType.\n");
        system.append("- Respect meta.structureType, meta.levels, meta.firstLevelCount.\n");
        system.append("- Use propertiesDesign.node (shapes, colorPalette, backgroundStrategy, iconPolicy) when present.\n");
        system.append("- Every add_node MUST include a 'description' in ").append(lang).append(".\n");
        system.append("- First-level nodes MUST have parentLabel = null.\n");
        system.append("- Order: set_title, set_structureType first; then add_node roots; then children.\n");

        StringBuilder user = new StringBuilder();
        user.append("OTMZ:\n");
        user.append(otmzJson).append("\n\n");
        user.append("Please:\n");
        user.append("1. First explain in ").append(lang).append(" what you're creating\n");
        user.append("2. Then return the Action List JSON: ");
        user.append("{\"actions\":[{\"type\":\"set_title\",\"params\":{\"title\":\"...\"}},");
        user.append("{\"type\":\"add_node\",\"params\":{\"parentLabel\":null,\"label\":\"...\",\"description\":\"...\",\"shape\":\"rectangle\",\"color\":\"#...\",\"background\":\"#...\",\"icon\":\"...\"}}]}");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system.toString())));
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString())));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);
        generationConfig.put("maxOutputTokens", 4000);

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
                        Map<String, Map<String, Object>> idToNode, StringBuilder output,
                        String indent, int depth) {
                // Limit depth to prevent overwhelming output
                if (depth > 5)
                        return;

                List<String> children = childrenMap.getOrDefault(nodeId, new ArrayList<>());
                for (String childId : children) {
                        Map<String, Object> childNode = idToNode.get(childId);
                        if (childNode == null) continue;

                        String childLabel = extractNodeLabel(childNode);
                        String props = extractNodeProperties(childNode);

                        output.append(indent).append("└─ ").append(childLabel)
                                        .append(" [ID: ").append(childId).append("] ")
                                        .append(props).append("\\n");
                        buildNodeHierarchy(childId, childrenMap, idToNode, output, indent + "  ", depth + 1);
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
         * Extract visual properties for context
         */
        private String extractNodeProperties(Map<String, Object> node) {
                StringBuilder props = new StringBuilder("{");
                Object type = node.get("type");
                if (type != null) props.append("type:").append(type).append(", ");
                
                Object data = node.get("data");
                if (data instanceof Map<?, ?> m) {
                        if (m.containsKey("shape")) props.append("shape:").append(m.get("shape")).append(", ");
                        if (m.containsKey("color")) props.append("color:").append(m.get("color")).append(", ");
                        if (m.containsKey("bgColor")) props.append("bg:").append(m.get("bgColor")).append(", ");
                }
                
                Object style = node.get("style");
                if (style instanceof Map<?, ?> m) {
                         if (m.containsKey("background")) props.append("bg:").append(m.get("background")).append(", ");
                         if (m.containsKey("color")) props.append("color:").append(m.get("color")).append(", ");
                }
                
                if (props.length() > 1) props.setLength(props.length() - 2); // remove last comma
                props.append("}");
                return props.toString();
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
                system.append("- Node properties: nodeType, shapes (VARY), colors, background, description, icons\\\\n");
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
                user.append("  \\\"title\\\": \\\"Mindmap Title (engaging, in ").append(language).append(")\\\",\\n");
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
                user.append("- USE DIVERSE SHAPES: Vary node shapes (rectangle, circle, diamond, hexagon, ellipse, roundedRectangle) for visual distinction\\n");
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
