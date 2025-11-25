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
                        List<String> nodeLabels,
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
                if (nodeLabels != null && !nodeLabels.isEmpty()) {
                        user.append("- Các node hiện có: ").append(String.join(", ", nodeLabels)).append("\\n");
                }
                user.append("\\n");

                if (hints != null && !hints.isEmpty()) {
                        user.append("Yêu cầu của người dùng:\\n");
                        user.append(String.join("\\n", hints)).append("\\n\\n");
                }

                user.append("Hãy:\n");
                user.append("1. QUAN TRỌNG: Giải thích bằng ngôn ngữ tự nhiên (").append(language)
                                .append(") chi tiết, thân thiện những gì bạn sẽ làm (2-3 câu)\n");
                user.append("2. Sau đó xuất JSON plan với format:\n");
                user.append("```json\n{\n");
                user.append("  \"targetType\": \"structure|description|node\",\n");
                user.append("  \"structureType\": \"mindmap|logic|brace|org|tree|timeline|fishbone\",\n");
                user.append("  \"language\": \"vi|en\",\n");
                user.append("  \"ops\": [\n");
                user.append(
                                "    {\"type\": \"add_node\", \"parentLabel\": \"...\", \"label\": \"...\"},\n");
                user.append(
                                "    {\"type\": \"update_node\", \"nodeLabel\": \"...\", \"newLabel\": \"...\"},\n");
                user.append("    {\"type\": \"delete_node\", \"nodeLabel\": \"...\"},\n");
                user.append("    {\"type\": \"delete_subtree\", \"nodeLabel\": \"...\"}  // Xóa node và tất cả node con\n");
                user.append("  ]\n}\n```\n");

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
                        int maxFirst) {
                StringBuilder system = new StringBuilder();
                system.append("Bạn là công cụ tạo mindmap. JSON ONLY, không text thừa. Quy tắc:\\n");
                system.append("- Tiêu đề node rõ ràng, 1-4 từ\\n");
                system.append("- FirstLevel:  ").append(firstLevelCount)
                                .append(" nhánh, PHẢI có parentId=null hoặc rỗng\\n");
                system.append("- Tổng phụ node/nhánh tối đa ").append(levels).append(" cấp, không quá sâu.\\n");
                system.append("- Đa dạng nội dung, tránh lặp\\n");
                system.append("- JSON chuẩn, không markdown```");

                StringBuilder user = new StringBuilder();
                user.append("Chủ đề: ").append(topic).append("\\n");
                user.append("Ngôn ngữ: ").append(language).append("\\n");
                user.append("Số cấp: ").append(levels).append("\\n");
                user.append("Số nhánh chính: ").append(firstLevelCount).append("\\n");
                if (tags != null && !tags.isEmpty()) {
                        user.append("Tags: ").append(String.join(", ", tags)).append("\\n");
                }
                user.append("Mode: ").append(mode).append("\\n");
                user.append(
                                "Tạo mindmap chi tiết, trả JSON: { \\\"nodes\\\": [ { \\\"id\\\": \\\"...\\\", \\\"label\\\": \\\"...\\\", \\\"parentId\\\": \\\"...\\\" } ] }");

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
}
