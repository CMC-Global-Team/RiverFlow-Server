package com.riverflow.service.mindmap.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.ai.Action;
import com.riverflow.dto.mindmap.ai.ActionList;
import com.riverflow.dto.mindmap.ai.Otmz;
import com.riverflow.dto.mindmap.ai.LoopPlan;
import com.riverflow.dto.mindmap.ai.LoopTask;
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
                        // ignore malformed op entries
                    }
                }
            }

            return new AiDecision(targetType, language, structureType, nodeLabel, ops);
        } catch (Exception e) {
            return new AiDecision(null, null, null, null, List.of());
        }
    }

    /**
     * Parse OTMZ (Thinking step output) into DTO
     */
    public Otmz parseOtmz(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            Otmz otmz = new Otmz();

            otmz.setMeta(nodeToMap(root.get("meta")));
            otmz.setPromptAnalysis(nodeToMap(root.get("promptAnalysis")));
            otmz.setPropertiesDesign(nodeToMap(root.get("propertiesDesign")));
            otmz.setOptimizedContent(nodeToMap(root.get("optimizedContent")));

            return otmz;
        } catch (Exception e) {
            // Return empty DTO on failure for resiliency
            return new Otmz();
        }
    }

    /**
     * Parse Action List (Agent step output) into DTO
     */
    public ActionList parseActionList(String json) {
        ActionList actionList = new ActionList();
        
        // Add null/empty check first
        if (json == null || json.trim().isEmpty()) {
            System.err.println("[parseActionList] Received null or empty JSON string");
            return actionList;
        }
        
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode actionsNode = root.get("actions");
            if (actionsNode != null && actionsNode.isArray()) {
                for (JsonNode a : actionsNode) {
                    try {
                        Action action = objectMapper.convertValue(a, Action.class);
                        if (action != null) {
                            actionList.getActions().add(action);
                        }
                    } catch (Exception e) {
                        System.err.println("[parseActionList] Failed to parse individual action: " + e.getMessage());
                        e.printStackTrace();
                        // skip invalid action entry
                    }
                }
            } else {
                System.err.println("[parseActionList] 'actions' array not found in JSON");
            }
        } catch (Exception e) {
            System.err.println("[parseActionList] Failed to parse JSON: " + e.getMessage());
            e.printStackTrace();
            // return empty list on failure
        }
        return actionList;
    }

    /**
     * Parse loop planner response into LoopPlan DTO.
     */
    public LoopPlan parseLoopPlan(String json) {
        LoopPlan plan = new LoopPlan();

        if (json == null || json.trim().isEmpty()) {
            return plan;
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode iterationsNode = root.get("iterations");
            if (iterationsNode != null && iterationsNode.isInt()) {
                plan.setIterations(iterationsNode.asInt());
            }

            JsonNode tasksNode = root.get("tasks");
            if (tasksNode != null && tasksNode.isArray()) {
                for (JsonNode t : tasksNode) {
                    try {
                        LoopTask task = objectMapper.convertValue(t, LoopTask.class);
                        if (task != null) {
                            plan.getTasks().add(task);
                        }
                    } catch (Exception ignored) {
                        // ignore malformed task entries to keep flow resilient
                    }
                }
            }
        } catch (Exception e) {
            // return empty plan on parse failure to avoid hard crash
        }

        return plan;
    }

    private Map<String, Object> nodeToMap(JsonNode node) {
        if (node == null || node.isNull()) return Map.of();
        try {
            return objectMapper.convertValue(node, Map.class);
        } catch (IllegalArgumentException ex) {
            return Map.of();
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
