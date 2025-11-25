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
        StringBuilder user = new StringBuilder();
        user.append("Bạn là hệ thống phân tích yêu cầu người dùng cho mindmap hiện tại.\\n");
        user.append("Tiêu đề: ").append(title).append("\\n");
        if (StringUtils.hasText(description)) {
            user.append("Mô tả: ").append(description).append("\\n");
        }
        if (nodeLabels != null && !nodeLabels.isEmpty()) {
            user.append("Các node hiện có: ").append(String.join(", ", nodeLabels)).append("\\n");
        }
        user.append(
                "Yêu cầu: Hãy xuất một kế hoạch (plan) chi tiết, dùng đúng ID/label node hiện có. Plan là JSON: {\\n  \\\"targetType\\\": \\\"structure|description|node\\\",\\n  \\\"structureType\\\": \\\"mindmap|logic|brace|org|tree|timeline|fishbone\\\",\\n  \\\"language\\\": \\\"vi|en\\\",\\n  \\\"ops\\\": [\\n    {\\\"type\\\": \\\"delete_node\\\", \\\"nodeLabel\\\": \\\"...\\\"},\\n    {\\\"type\\\": \\\"delete_subtree\\\", \\\"nodeLabel\\\": \\\"...\\\"},\\n    {\\\"type\\\": \\\"update_node\\\", \\\"nodeLabel\\\": \\\"...\\\", \\\"newLabel\\\": \\\"...\\\"},\\n    {\\\"type\\\": \\\"add_node\\\", \\\"parentLabel\\\": \\\"...\\\", \\\"label\\\": \\\"...\\\"},\\n    {\\\"type\\\": \\\"add_edge\\\", \\\"sourceLabel\\\": \\\"...\\\", \\\"targetLabel\\\": \\\"...\\\"}\\n  ]\\n}\\n");
        user.append(
                "Nếu người dùng nói rõ Thêm/Sửa/Xóa/Cập nhật thì plan phải nêu chính xác node/edge liên quan theo label/ID hiện có. Nếu không chắc, hãy chọn nhánh gốc (ROOT) làm parentLabel.\\n");
        if (hints != null && !hints.isEmpty()) {
            user.append("Yêu cầu người dùng: ").append(String.join(" \\n ", hints)).append("\\n");
        }
        user.append("Nếu người dùng có ưu tiên về cấu trúc/ ngôn ngữ thì hãy ưu tiên theo lựa chọn đó.\\n");
        user.append(
                "Hãy quyết định hành động chính (targetType) và xuất \\\"ops\\\" chi tiết như mẫu trên, chỉ trả JSON hợp lệ.\\n");

        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user.toString())));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2);

        Map<String, Object> payload = new HashMap<>();
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
        system.append("- FirstLevel:  ").append(firstLevelCount).append(" nhánh, PHẢI có parentId=null hoặc rỗng\\n");
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
     */
    public String ensureJson(String text) {
        if (text == null)
            return null;
        String s = text.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1).trim();
        }
        return s;
    }
}
