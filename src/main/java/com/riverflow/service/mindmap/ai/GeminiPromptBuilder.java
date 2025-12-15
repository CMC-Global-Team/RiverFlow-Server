package com.riverflow.service.mindmap.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

import com.riverflow.dto.mindmap.ai.Otmz;
import com.riverflow.dto.mindmap.ai.EvaluationResult;
import com.riverflow.model.mindmap.Mindmap;

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
                double temp = "max".equalsIgnoreCase(mode) ? 1.0 : 0.7;
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
     * Build prompt for Thinking Mode - AI analyzes user's raw prompt
     * and returns optimized specification for mindmap generation
     */
    public Map<String, Object> buildThinkingModePrompt(
            String userPrompt,
            String language,
            List<String> tags,
            String preferredStructure,
            String complexity) {
        
        StringBuilder system = new StringBuilder();
        system.append("You are an AI Thinking Assistant for mindmap creation. Your role:\n");
        system.append("1. FIRST: Explain in natural language (").append(language).append(") what you understand and plan\n");
        system.append("2. THEN: Output optimized JSON specification\n");
        system.append("Format: <natural language explanation>\n\n```json\n<optimized spec>\n```\n");

        StringBuilder user = new StringBuilder();
        user.append("User's raw prompt:\n");
        user.append("\"").append(userPrompt).append("\"\n\n");
        
        // Assuming StringUtils.hasText is available or replaced with a check for null/empty
        // For this context, I'll assume it's available or a simple check is sufficient.
        // If StringUtils is not imported, it would cause a compilation error.
        // For faithful reproduction, I'll keep it as is.
        if (preferredStructure != null && !preferredStructure.trim().isEmpty()) { // Replaced StringUtils.hasText
            user.append("Preferred structure: ").append(preferredStructure).append("\n");
        }
        if (tags != null && !tags.isEmpty()) {
            user.append("User-provided tags: ").append(String.join(", ", tags)).append("\n");
        }
        user.append("Complexity preference: ").append(complexity).append("\n");
        user.append("Language: ").append(language).append("\n\n");

        user.append("Your tasks:\n");
        user.append("1. Analyze the user's intent and needs\n");
        user.append("2. Explain in ").append(language).append(" (2-3 sentences) what you understood and what you will create\n");
        user.append("3. Then output JSON with this format:\n");
        user.append("```json\n{\n");
        user.append("  \"optimizedTopic\": \"Clear, focused topic extracted from prompt\",\n");
        user.append("  \"optimizedTitle\": \"Engaging title for the mindmap\",\n");
        user.append("  \"structureType\": \"mindmap|logic|brace|org|tree|timeline|fishbone (best fit for content)\",\n");
        user.append("  \"levels\": 2,  // Recommended depth (1-3)\n");
        user.append("  \"firstLevelCount\": 5,  // Recommended number of main branches (3-6)\n");
        user.append("  \"tags\": [\"tag1\", \"tag2\"],  // Extracted/refined tags\n");
        user.append("  \"language\": \"").append(language).append("\",\n");
        user.append("  \"actionList\": [\n");
        user.append("    \"Action 1: What the Agent should do first\",\n");
        user.append("    \"Action 2: What the Agent should do next\",\n");
        user.append("    \"Action 3: Final steps\"\n");
        user.append("  ],\n");
        user.append("  \"reasoning\": \"Why these choices were made\",\n");
        user.append("  \"additionalProperties\": {\n");
        user.append("    \"focusAreas\": [\"area1\", \"area2\"],\n");
        user.append("    \"tone\": \"professional|casual|educational\",\n");
        user.append("    \"visualStyle\": \"colorful|minimal|business\"\n");
        user.append("  }\n");
        user.append("}\n```\n\n");

        user.append("Guidelines:\n");
        user.append("- Extract the core topic and intent from the user's prompt\n");
        user.append("- Choose the structure type that best fits the content\n");
        user.append("- Create a clear action list (3-5 items) for the Agent to follow\n");
        user.append("- Optimize parameters (levels, firstLevelCount) based on topic complexity\n");
        user.append("- Provide reasoning for your decisions\n");
        user.append("- All text in ").append(language).append("\n");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system.toString())));
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString())));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4); // Balanced between creativity and precision
        generationConfig.put("maxOutputTokens", 4000);

        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    /**
     * Build prompt for loop planner (Max mode agent)
     */
    public Map<String, Object> buildLoopPlannerPrompt(
            String prompt,
            String language,
            Map<String, Object> workspace,
            Integer maxIterations) {

        int iterations = (maxIterations != null && maxIterations > 0) ? maxIterations : 1;

        StringBuilder system = new StringBuilder();
        system.append("You are an AI planner that breaks a mindmap request into iterative tasks.\n");
        system.append("Return JSON only. Format:\n");
        system.append("{\n");
        system.append("  \"iterations\": 2,\n");
        system.append("  \"tasks\": [\n");
        system.append("    {\"id\":\"t1\",\"topic\":\"...\",\"structureType\":\"mindmap|logic|brace|org|tree|timeline|fishbone\",");
        system.append("\"levels\":2,\"firstLevelCount\":5,\"tags\":[\"...\"],\"dependsOn\":[\"tX\"]}\n");
        system.append("  ]\n");
        system.append("}\n");

        StringBuilder user = new StringBuilder();
        user.append("User prompt: ").append(prompt).append("\n");
        user.append("Language: ").append(language).append("\n");
        if (workspace != null && !workspace.isEmpty()) {
            user.append("Workspace context: ").append(workspace).append("\n");
        }
        user.append("Max iterations: ").append(iterations).append("\n");
        user.append("Please plan up to this number of iterations and tasks.\n");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system.toString())));
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString())));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);

        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    /**
     * Build prompt for evaluation step (MaxMode).
     * AI returns JSON with score, issues, and refinementHint.
     */
    public Map<String, Object> buildEvaluationPrompt(
            Mindmap mindmap,
            String originalPrompt,
            String language,
            String structureType,
            Integer levels,
            Integer firstLevelCount) {

        String safeLang = StringUtils.hasText(language) ? language : "vi";
        String safeStructure = StringUtils.hasText(structureType) ? structureType : "mindmap";

        StringBuilder system = new StringBuilder();
        system.append("You are a strict mindmap quality evaluator.\n");
        system.append("Return PURE JSON only, no markdown.\n");
        system.append("JSON schema:\n");
        system.append("{\n");
        system.append("  \"score\": 0.0-1.0,\n");
        system.append("  \"summary\": \"short evaluation in ").append(safeLang).append("\",\n");
        system.append("  \"refinementHint\": \"optional guidance in ").append(safeLang).append("\",\n");
        system.append("  \"issues\": [\n");
        system.append("    {\n");
        system.append("      \"type\": \"DUPLICATE_BRANCH|DEPTH_IMBALANCE|OFF_TOPIC|WEAK_LEAF|STRUCTURE_MISMATCH\",\n");
        system.append("      \"message\": \"description in ").append(safeLang).append("\",\n");
        system.append("      \"severity\": \"info|warning|error\",\n");
        system.append("      \"nodeIds\": [\"optional-node-id\"],\n");
        system.append("      \"suggestion\": \"how to fix, in ").append(safeLang).append("\"\n");
        system.append("    }\n");
        system.append("  ]\n");
        system.append("}\n");

        StringBuilder user = new StringBuilder();
        user.append("Original user prompt: ").append(originalPrompt).append("\n");
        user.append("Language: ").append(safeLang).append("\n");
        user.append("Desired structureType: ").append(safeStructure).append("\n");
        user.append("Desired levels: ").append(levels != null ? levels : "unknown").append("\n");
        user.append("Desired firstLevelCount: ").append(firstLevelCount != null ? firstLevelCount : "unknown").append("\n\n");
        user.append("Current mindmap JSON:\n");
        user.append(String.valueOf(mindmap != null ? mindmap : "null")).append("\n\n");
        user.append("Evaluate along these axes:\n");
        user.append("- structural balance (levels, branch distribution)\n");
        user.append("- duplicate or overlapping branches\n");
        user.append("- off-topic or irrelevant nodes\n");
        user.append("- clarity and atomicity of leaf nodes\n");
        user.append("- alignment with desired structure type\n");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system.toString())));
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString())));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", 4000);

        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    /**
     * Build prompt for loop THINKING step (produces OTMZ JSON).
     * This is an internal, JSON-only response used by the orchestrator.
     */
    public Map<String, Object> buildLoopThinkingPrompt(
            String topic,
            String language,
            String structureType,
            Integer levels,
            Integer firstLevelCount,
            java.util.List<String> tags,
            String mode) {

        String safeLang = StringUtils.hasText(language) ? language : "vi";
        String safeStructure = StringUtils.hasText(structureType) ? structureType : "mindmap";

        StringBuilder system = new StringBuilder();
        system.append("You are an internal thinking module for a mindmap AI agent.\n");
        system.append("You MUST return pure JSON OTMZ object, no markdown, no natural language around it.\n");
        system.append("Your job: analyze the topic and design optimized mindmap parameters.\n");
        system.append("JSON schema:\n");
        system.append("{\n");
        system.append("  \"meta\": {\n");
        system.append("    \"language\": \"").append(safeLang).append("\",\n");
        system.append("    \"mode\": \"").append(mode).append("\",\n");
        system.append("    \"structureType\": \"").append(safeStructure).append("\"\n");
        system.append("  },\n");
        system.append("  \"promptAnalysis\": {\n");
        system.append("    \"goal\": \"Short description of the learning goal in ").append(safeLang).append("\",\n");
        system.append("    \"targetAudience\": \"Who this mindmap is for\",\n");
        system.append("    \"difficulty\": \"beginner|intermediate|advanced\"\n");
        system.append("  },\n");
        system.append("  \"propertiesDesign\": {\n");
        system.append("    \"topic\": \"Optimized topic string\",\n");
        system.append("    \"title\": \"Optimized title for the mindmap\",\n");
        system.append("    \"structureType\": \"mindmap|logic|brace|org|tree|timeline|fishbone\",\n");
        system.append("    \"levels\": 2,\n");
        system.append("    \"firstLevelCount\": 4,\n");
        system.append("    \"tags\": [\"...\"]\n");
        system.append("  },\n");
        system.append("  \"optimizedContent\": {\n");
        system.append("    \"mainBranches\": [\n");
        system.append("      {\"label\": \"Branch 1\", \"description\": \"...\"}\n");
        system.append("    ]\n");
        system.append("  }\n");
        system.append("}\n");

        StringBuilder user = new StringBuilder();
        user.append("Topic: ").append(topic).append("\n");
        user.append("Language: ").append(safeLang).append("\n");
        user.append("Requested structure: ").append(safeStructure).append("\n");
        if (levels != null) {
            user.append("Requested levels: ").append(levels).append("\n");
        }
        if (firstLevelCount != null) {
            user.append("Requested firstLevelCount: ").append(firstLevelCount).append("\n");
        }
        if (tags != null && !tags.isEmpty()) {
            user.append("Tags: ").append(String.join(", ", tags)).append("\n");
        }
        user.append("Mode: ").append(mode).append("\n\n");
        user.append("Please fill all OTMZ fields with consistent, non-overlapping branches.\n");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system.toString())));
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString())));

        Map<String, Object> generationConfig = new HashMap<>();
        double temp = "max".equalsIgnoreCase(mode) ? 0.8 : 0.5;
        generationConfig.put("temperature", temp);
        generationConfig.put("maxOutputTokens", 4000);

        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    /**
     * Build prompt for AGENT planning step.
     * Consumes OTMZ and produces ActionList JSON with concrete operations.
     */
    public Map<String, Object> buildAgentActionPrompt(
            Otmz otmz,
            String language) {

        String safeLang = StringUtils.hasText(language) ? language : "vi";

        StringBuilder system = new StringBuilder();
        system.append("You are an AI agent that converts an internal OTMZ analysis into concrete mindmap actions.\n");
        system.append("Return PURE JSON with this shape (no markdown, no extra text):\n");
        system.append("{\n");
        system.append("  \"actions\": [\n");
        system.append("    {\n");
        system.append("      \"id\": \"a1\",\n");
        system.append("      \"type\": \"add_node|update_node|delete_node|delete_subtree|add_edge|set_title|set_structureType\",\n");
        system.append("      \"params\": { ... },\n");
        system.append("      \"rationale\": \"Short reason in ").append(safeLang).append("\",\n");
        system.append("      \"priority\": 1,\n");
        system.append("      \"dependsOn\": [\"a0\"]\n");
        system.append("    }\n");
        system.append("  ]\n");
        system.append("}\n");

        StringBuilder user = new StringBuilder();
        user.append("Language: ").append(safeLang).append("\n");
        user.append("Here is the OTMZ object (internal thinking result):\n");
        user.append(otmz != null ? otmz.toString() : "{}").append("\n\n");
        user.append("Rules:\n");
        user.append("- Actions at the same level MUST NOT overlap or contradict each other.\n");
        user.append("- Prefer clear, atomic operations (each action changes one thing).\n");
        user.append("- Use set_title and set_structureType at most once.\n");
        user.append("- When adding nodes, always provide label, description, and visual properties if possible.\n");

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", system.toString())));
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString())));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.5);
        generationConfig.put("maxOutputTokens", 4000);

        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);
        return payload;
    }

    /**
     * Build prompt for refinement step.
     * Takes current mindmap + evaluation result and returns ActionList JSON.
     */
    public Map<String, Object> buildRefinementPrompt(
            Mindmap mindmap,
            EvaluationResult evaluation,
            String language) {

        String safeLang = StringUtils.hasText(language) ? language : "vi";

        StringBuilder system = new StringBuilder();
        system.append("You are a refinement agent for a mindmap AI.\n");
        system.append("Your task is to FIX the issues listed in evaluation by producing concrete actions.\n");
        system.append("Return PURE JSON in this shape (no markdown):\n");
        system.append("{\n");
        system.append("  \"actions\": [\n");
        system.append("    {\n");
        system.append("      \"id\": \"r1\",\n");
        system.append("      \"type\": \"add_node|update_node|delete_node|delete_subtree|add_edge|set_title|set_structureType\",\n");
        system.append("      \"params\": { ... },\n");
        system.append("      \"rationale\": \"short reason in ").append(safeLang).append("\",\n");
        system.append("      \"priority\": 1,\n");
        system.append("      \"dependsOn\": []\n");
        system.append("    }\n");
        system.append("  ]\n");
        system.append("}\n");

        StringBuilder user = new StringBuilder();
        user.append("Language: ").append(safeLang).append("\n");
        user.append("Current mindmap object:\n");
        user.append(String.valueOf(mindmap != null ? mindmap : "null")).append("\n\n");
        user.append("Evaluation result:\n");
        user.append(String.valueOf(evaluation != null ? evaluation : "null")).append("\n\n");
        user.append("Please output the minimal set of high-impact actions that:\n");
        user.append("- remove or merge duplicate branches,\n");
        user.append("- balance depth where needed,\n");
        user.append("- delete or reword off-topic nodes,\n");
        user.append("- clarify weak leaf nodes.\n");

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
