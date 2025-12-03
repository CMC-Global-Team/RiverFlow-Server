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

    /**
     * Parse Max Mode planner output (iterations + tasks[])
     */
    public LoopPlan parseLoopPlan(String json) {
        LoopPlan plan = new LoopPlan();
        try {
            JsonNode root = objectMapper.readTree(json);
            // iterations
            JsonNode itNode = root.get("iterations");
            if (itNode != null && itNode.isNumber()) {
                plan.setIterations(itNode.asInt());
            } else {
                plan.setIterations(1);
            }

            // tasks
            JsonNode tasksNode = root.get("tasks");
            if (tasksNode != null && tasksNode.isArray()) {
                int idx = 1;
                for (JsonNode t : tasksNode) {
                    try {
                        LoopTask task = new LoopTask();
                        String id = textOrNull(t.get("id"));
                        if (id == null || id.isBlank()) id = "t" + idx;
                        task.setId(id);
                        task.setTitle(textOrNull(t.get("title")));
                        task.setDescription(textOrNull(t.get("description")));

                        // dependsOn
                        List<String> deps = new ArrayList<>();
                        JsonNode depsNode = t.get("dependsOn");
                        if (depsNode != null) {
                            if (depsNode.isArray()) {
                                for (JsonNode d : depsNode) {
                                    String v = textOrNull(d);
                                    if (v != null && !v.isBlank()) deps.add(v);
                                }
                            } else {
                                String v = textOrNull(depsNode);
                                if (v != null && !v.isBlank()) deps.add(v);
                            }
                        }
                        if (!deps.isEmpty()) task.setDependsOn(deps);

                        plan.getTasks().add(task);
                        idx++;
                    } catch (Exception ignore) {
                        // skip invalid task entry
                    }
                }
            }
        } catch (Exception e) {
            // return best-effort parsed plan (may be empty)
            if (plan.getIterations() == null) plan.setIterations(1);
        }
        return plan;
    }

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
                    } catch (Exception ignore) {
                        // skip invalid action entry
                    }
                }
            }
        } catch (Exception e) {
            // return empty list on failure
        }
        return actionList;
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
