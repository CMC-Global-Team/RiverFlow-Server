package com.riverflow.service.mindmap.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.ai.ActionList;
import com.riverflow.dto.mindmap.ai.Otmz;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiThinkingModeServiceImpl implements AiThinkingModeService {

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;
    private final GeminiPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;
    private final ObjectMapper objectMapper;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Override
    public Otmz think(String topic, String language, String structureType, Integer levels, Integer firstLevelCount, List<String> tags, String mode) {
        Map<String, Object> payload = promptBuilder.buildThinkingOtmzPrompt(
                topic, language, structureType, levels, firstLevelCount, tags, mode
        );
        String text = callGemini(payload);
        String json = promptBuilder.ensureJson(text);
        return responseParser.parseOtmz(json);
    }

    @Override
    public ActionList plan(Otmz otmz, String language) {
        try {
            String otmzJson = objectMapper.writeValueAsString(otmz);
            Map<String, Object> payload = promptBuilder.buildActionListPrompt(otmzJson, language);
            String text = callGemini(payload);
            String json = promptBuilder.ensureJson(text);
            return responseParser.parseActionList(json);
        } catch (Exception e) {
            return new ActionList();
        }
    }

    // Basic non-streaming Gemini call
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
