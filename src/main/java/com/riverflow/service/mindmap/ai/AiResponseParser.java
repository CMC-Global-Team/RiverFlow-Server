package com.riverflow.service.mindmap.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Parses Gemini AI JSON responses into clean Java objects
 */
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * Parse the AI classify/plan response
     */
    public AiDecision parseClassifyResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String targetType = textOrNull(root.get("targetType"));
            String language = textOrNull(root.get("language"));
            String structureType = textOrNull(root.get("structureType"));
            String nodeLabel = textOrNull(root.get("nodeLabel"));

            List<Map<String, Object>> ops = new ArrayList<>();
            JsonNode opsNode = root.get("ops");
            if (opsNode != null && opsNode.isArray()) {
                for (JsonNode op : opsNode) {
                    try {
                        Map<String, Object> opMap = objectMapper.convertValue(op, Map.class);
                        ops.add(opMap);
                    } catch (Exception e) {
                        }
                }
            }

            return new AiDecision(targetType, language, structureType, nodeLabel, ops);
        } catch (Exception e) {
            return new AiDecision(null, null, null, null, List.of());
        }
    }

    /**
     * AI decision result
     */
    public record AiDecision(
            String targetType,
            String language,
            String structureType,
            String nodeLabel,
            List<Map<String, Object>> ops) {
        public boolean hasTargetType() {
            return StringUtils.hasText(targetType);
        }

        public boolean hasOps() {
            return ops != null && !ops.isEmpty();
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull())
            return null;
        return node.isTextual() ? node.asText() : node.toString();
    }
}
