package com.riverflow.service.mindmap.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.ai.ActionList;
import com.riverflow.dto.mindmap.ai.EvaluationResult;
import com.riverflow.model.mindmap.Mindmap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Implementation of strong evaluation & refinement for MaxMode.
 * Uses Gemini with dedicated prompts to:
 * - score the current mindmap
 * - detect structural / semantic issues
 * - generate refinement operations when needed.
 */
@Service
@RequiredArgsConstructor
public class AiEvaluationServiceImpl implements AiEvaluationService {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;
    private final GeminiPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;
    private final ObjectMapper objectMapper;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Override
    public EvaluationResult evaluate(Mindmap mindmap,
                                     String originalPrompt,
                                     String language,
                                     String structureType,
                                     Integer levels,
                                     Integer firstLevelCount) {

        Map<String, Object> payload = promptBuilder.buildEvaluationPrompt(
                mindmap,
                originalPrompt,
                language,
                structureType,
                levels,
                firstLevelCount
        );

        String text = callGemini(payload);
        String json = promptBuilder.ensureJson(text);

        try {
            JsonNode root = objectMapper.readTree(json);
            EvaluationResult result = new EvaluationResult();

            JsonNode scoreNode = root.get("score");
            if (scoreNode != null && scoreNode.isNumber()) {
                result.setScore(scoreNode.doubleValue());
            }

            JsonNode summaryNode = root.get("summary");
            if (summaryNode != null && summaryNode.isTextual()) {
                result.setSummary(summaryNode.asText());
            }

            JsonNode hintNode = root.get("refinementHint");
            if (hintNode != null && hintNode.isTextual()) {
                result.setRefinementHint(hintNode.asText());
            }

            JsonNode issuesNode = root.get("issues");
            if (issuesNode != null && issuesNode.isArray()) {
                issuesNode.forEach(issueNode -> {
                    try {
                        result.getIssues().add(
                                objectMapper.treeToValue(issueNode, com.riverflow.dto.mindmap.ai.EvaluationIssue.class)
                        );
                    } catch (Exception ignored) {
                        // skip malformed issues
                    }
                });
            }

            return result;
        } catch (Exception e) {
            // Fail-soft: return empty result if parsing fails
            return new EvaluationResult();
        }
    }

    @Override
    public ActionList refine(Mindmap mindmap,
                             EvaluationResult evaluation,
                             String language) {

        Map<String, Object> payload = promptBuilder.buildRefinementPrompt(
                mindmap,
                evaluation,
                language
        );

        String text = callGemini(payload);
        String json = promptBuilder.ensureJson(text);

        return responseParser.parseActionList(json);
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
            if (!(candidatesObj instanceof java.util.List<?> candidates) || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response");
            }

            Object contentObj = ((Map<?, ?>) candidates.get(0)).get("content");
            if (!(contentObj instanceof Map<?, ?> content)) {
                throw new RuntimeException("Invalid Gemini response: missing content");
            }
            Object partsObj = content.get("parts");
            if (!(partsObj instanceof java.util.List<?> parts) || parts.isEmpty()) {
                throw new RuntimeException("Invalid Gemini response: missing parts");
            }
            Object text = ((Map<?, ?>) parts.get(0)).get("text");
            return text == null ? null : String.valueOf(text);
        } catch (Exception e) {
            throw new RuntimeException("AI evaluation service failed: " + e.getMessage());
        }
    }
}


