package com.riverflow.service.mindmap.ai;

import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.UpdateMindmapRequest;
import com.riverflow.dto.mindmap.ai.Action;
import com.riverflow.dto.mindmap.ai.ActionList;
import com.riverflow.dto.mindmap.ai.LoopPlan;
import com.riverflow.dto.mindmap.ai.LoopTask;
import com.riverflow.dto.mindmap.ai.Otmz;
import com.riverflow.dto.mindmap.ai.EvaluationResult;
import com.riverflow.exception.mindmap.MindmapAccessDeniedException;
import com.riverflow.exception.mindmap.MindmapNotFoundException;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.service.mindmap.MindmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiLoopOrchestratorImpl implements AiLoopOrchestrator {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;
    private final GeminiPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;
    private final AiThinkingModeService thinkingService;
    private final AiOperationExecutor operationExecutor;
    private final MindmapRepository mindmapRepository;
    private final MindmapService mindmapService;
    private final LayoutEngine layoutEngine;
    private final AiEvaluationService evaluationService;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Override
    public MindmapResponse run(String mindmapId,
                               Long userId,
                               String prompt,
                               String language,
                               Map<String, Object> workspace,
                               Integer maxIterations,
                               String mode) {
        // 1) Load mindmap and check access
        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));
        validatePermissions(mindmap, userId);

        String lang = StringUtils.hasText(language) ? language : "vi";
        String safeMode = (mode == null || mode.isBlank()) ? "normal" : mode;

        // 2) Plan tasks
        Map<String, Object> plannerPayload = promptBuilder.buildLoopPlannerPrompt(prompt, lang, workspace, maxIterations);
        String planText = callGemini(plannerPayload);
        String planJson = promptBuilder.ensureJson(planText);
        LoopPlan plan = responseParser.parseLoopPlan(planJson);
        if (plan.getIterations() == null || plan.getIterations() < 1) plan.setIterations(1);

        // 3) Execute loop
        int iterations = plan.getIterations();
        String lastStructureType = "mindmap";
        String pendingTitle = null;
        double qualityThreshold = "max".equalsIgnoreCase(safeMode) ? 0.85 : 0.8;
        int maxRefinementRounds = "max".equalsIgnoreCase(safeMode) ? 3 : 1;

        for (int i = 0; i < iterations; i++) {
            List<LoopTask> tasks = plan.getTasks();
            if (tasks == null || tasks.isEmpty()) break;

            for (LoopTask t : tasks) {
                // Respect dependencies: ensure dependsOn are in earlier list; since we don't track completion per id here,
                // we process sequentially; simple approach suffices for MVP.

                // 3a) THINK
                Otmz otmz = thinkingService.think(
                        StringUtils.hasText(t.getTopic()) ? t.getTopic() : prompt,
                        lang,
                        t.getStructureType(),
                        t.getLevels(),
                        t.getFirstLevelCount(),
                        t.getTags(),
                        safeMode
                );

                // 3b) PLAN → actions
                ActionList actions = thinkingService.plan(otmz, lang);

                // 3c) Apply actions
                // Special handling for set_title and set_structureType
                List<Map<String, Object>> ops = new ArrayList<>();
                if (actions != null && actions.getActions() != null) {
                    for (Action a : actions.getActions()) {
                        String type = a.getType();
                        Map<String, Object> params = a.getParams();
                        if ("set_title".equalsIgnoreCase(type) && params != null) {
                            Object title = params.get("title");
                            if (title != null) pendingTitle = String.valueOf(title);
                            continue;
                        }
                        if ("set_structureType".equalsIgnoreCase(type) && params != null) {
                            Object st = params.get("structureType");
                            if (st != null && StringUtils.hasText(String.valueOf(st))) {
                                lastStructureType = String.valueOf(st);
                            }
                            continue;
                        }
                        // Convert to executor op (flatten params)
                        Map<String, Object> op = new HashMap<>();
                        op.put("type", type);
                        if (params != null) op.putAll(params);
                        ops.add(op);
                    }
                }

                if (!ops.isEmpty()) {
                    operationExecutor.executeOperations(ops, mindmap);
                }

                // 3d) Apply layout per task to keep graph tidy
                layoutEngine.applyLayout(
                        StringUtils.hasText(t.getStructureType()) ? t.getStructureType() : lastStructureType,
                        mindmap.getNodes(),
                        mindmap.getEdges()
                );

                // Save progress after each task
                UpdateMindmapRequest updateReq = UpdateMindmapRequest.builder()
                        .title(pendingTitle != null ? pendingTitle : mindmap.getTitle())
                        .nodes(mindmap.getNodes())
                        .edges(mindmap.getEdges())
                        .build();
                mindmapService.updateMindmap(mindmap.getId(), updateReq, userId);
            }
        }

        // 4) MaxMode-only: evaluation + refinement loop
        if ("max".equalsIgnoreCase(safeMode)) {
            int refinementRounds = 0;
            while (refinementRounds < maxRefinementRounds) {
                EvaluationResult evaluation = evaluationService.evaluate(
                        mindmap,
                        prompt,
                        lang,
                        lastStructureType,
                        plan.getIterations(),
                        null
                );

                Double score = evaluation.getScore();
                if (score != null && score >= qualityThreshold) {
                    break;
                }

                ActionList refineActions = evaluationService.refine(mindmap, evaluation, lang);
                if (refineActions == null || refineActions.isEmpty() || refineActions.getActions().isEmpty()) {
                    break;
                }

                List<Map<String, Object>> refineOps = new ArrayList<>();
                for (Action a : refineActions.getActions()) {
                    Map<String, Object> op = new HashMap<>();
                    op.put("type", a.getType());
                    if (a.getParams() != null) {
                        op.putAll(a.getParams());
                    }
                    refineOps.add(op);
                }

                if (!refineOps.isEmpty()) {
                    operationExecutor.executeOperations(refineOps, mindmap);
                    layoutEngine.applyLayout(
                            lastStructureType,
                            mindmap.getNodes(),
                            mindmap.getEdges()
                    );

                    UpdateMindmapRequest refineUpdate = UpdateMindmapRequest.builder()
                            .title(pendingTitle != null ? pendingTitle : mindmap.getTitle())
                            .nodes(mindmap.getNodes())
                            .edges(mindmap.getEdges())
                            .build();
                    mindmapService.updateMindmap(mindmap.getId(), refineUpdate, userId);
                }

                refinementRounds++;
            }
        }

        // Final save and return
        UpdateMindmapRequest finalUpdate = UpdateMindmapRequest.builder()
                .title(pendingTitle != null ? pendingTitle : mindmap.getTitle())
                .nodes(mindmap.getNodes())
                .edges(mindmap.getEdges())
                .build();
        return mindmapService.updateMindmap(mindmap.getId(), finalUpdate, userId);
    }

    private void validatePermissions(Mindmap mindmap, Long userId) {
        boolean isOwner = mindmap.getMysqlUserId() != null && mindmap.getMysqlUserId().equals(userId);
        boolean isEditor = mindmap.getCollaborators() != null && mindmap.getCollaborators().stream()
                .anyMatch(c -> Objects.equals(c.getMysqlUserId(), userId)
                        && "accepted".equals(c.getStatus())
                        && "EDITOR".equals(c.getRole()));
        if (!isOwner && !isEditor) {
            throw new MindmapAccessDeniedException(mindmap.getId(), userId);
        }
    }

    private String callGemini(Map<String, Object> payload) {
        try {
            String url = "/v1beta/models/" + model + ":generateContent";
            Map<?, ?> response = geminiWebClient.post()
                    .uri(url)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Gemini API returned null");
            }

            Object candidatesObj = response.get("candidates");
            if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response");
            }

            Object contentObj = ((Map<?, ?>) candidates.get(0)).get("content");
            if (!(contentObj instanceof Map<?, ?> content)) {
                throw new RuntimeException("Invalid Gemini response: missing content");
            }
            Object partsObj = content.get("parts");
            if (!(partsObj instanceof List<?> parts) || parts.isEmpty()) {
                throw new RuntimeException("Invalid Gemini response: missing parts");
            }
            Object text = ((Map<?, ?>) parts.get(0)).get("text");
            return text == null ? null : String.valueOf(text);
        } catch (Exception e) {
            throw new RuntimeException("AI service failed: " + e.getMessage());
        }
    }
}




