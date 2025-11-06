package com.riverflow.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.AiMindmapRequest;
import com.riverflow.dto.mindmap.ContextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI LLM Service Implementation
 * 
 * Requires OpenAI API key in application.properties:
 * app.llm.openai.api-key=your-api-key-here
 * app.llm.openai.model=gpt-4o-mini (or gpt-4, gpt-3.5-turbo)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAILlmService implements LlmService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${app.llm.openai.api-key:}")
    private String apiKey;
    
    @Value("${app.llm.openai.model:gpt-4o-mini}")
    private String model;
    
    @Value("${app.llm.openai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Override
    public boolean isAvailable() {
        boolean available = apiKey != null && !apiKey.isEmpty();
        log.info("LLM Service availability check: available={}, apiKey length={}", 
            available, apiKey != null ? apiKey.length() : 0);
        if (available) {
            log.info("Using OpenAI model: {}", model);
        } else {
            log.warn("OpenAI API key not configured or empty");
        }
        return available;
    }
    
    @Override
    public String generateMindmapContent(AiMindmapRequest request) {
        if (!isAvailable()) {
            log.warn("OpenAI API key not configured, returning empty response");
            return "{}";
        }
        
        try {
            log.info("Calling OpenAI API with model: {}", model);
            String prompt = buildPrompt(request);
            log.debug("OpenAI prompt: {}", prompt);
            String response = callOpenAI(prompt);
            log.info("OpenAI response received, length: {}", response != null ? response.length() : 0);
            String parsedResponse = parseOpenAIResponse(response);
            log.info("Parsed OpenAI response: {}", parsedResponse);
            return parsedResponse;
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return "{}";
        }
    }
    
    private String buildPrompt(AiMindmapRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là AI Mindmap Assistant. Tạo mindmap dựa trên yêu cầu sau:\n\n");
        
        if (request.getNodeTitle() != null) {
            prompt.append("Node hiện tại: ").append(request.getNodeTitle()).append("\n");
        }
        
        if (request.getNodeSummary() != null) {
            prompt.append("Tóm tắt: ").append(request.getNodeSummary()).append("\n");
        }
        
        if (request.getContextNodes() != null && !request.getContextNodes().isEmpty()) {
            prompt.append("Context nodes:\n");
            for (ContextNode ctx : request.getContextNodes()) {
                prompt.append("- ").append(ctx.getSummary()).append("\n");
            }
        }
        
        prompt.append("\nYêu cầu: ").append(request.getUserInstruction()).append("\n\n");
        prompt.append("Trả về JSON với format:\n");
        prompt.append("{\n");
        prompt.append("  \"nodes\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"id\": \"1\",\n");
        prompt.append("      \"type\": \"rectangle\",\n");
        prompt.append("      \"position\": {\"x\": 280, \"y\": 119},\n");
        prompt.append("      \"data\": {\n");
        prompt.append("        \"label\": \"Subject\",\n");
        prompt.append("        \"description\": \"\",\n");
        prompt.append("        \"color\": \"#3b82f6\",\n");
        prompt.append("        \"shape\": \"rectangle\"\n");
        prompt.append("      },\n");
        prompt.append("      \"width\": 150,\n");
        prompt.append("      \"height\": 48\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"edges\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"id\": \"e1-2\",\n");
        prompt.append("      \"source\": \"1\",\n");
        prompt.append("      \"target\": \"2\",\n");
        prompt.append("      \"animated\": false,\n");
        prompt.append("      \"type\": \"smoothstep\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");
        prompt.append("Quy tắc:\n");
        prompt.append("- Nếu yêu cầu 'expand' hoặc 'mở rộng': tạo 2-4 node con\n");
        prompt.append("- Nếu yêu cầu 'summarize' hoặc 'tóm tắt': tạo cấu trúc tóm tắt ngắn gọn\n");
        prompt.append("- Nếu yêu cầu 'add' hoặc 'thêm': thêm nodes mới dựa trên instruction\n");
        prompt.append("- Giữ mỗi node label ngắn gọn (≤30 ký tự)\n");
        prompt.append("- Chỉ trả về JSON, không có text khác\n");
        
        return prompt.toString();
    }
    
    private String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "You are a helpful AI assistant that generates mindmap JSON structures. Always respond with valid JSON only."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);
        requestBody.put("response_format", Map.of("type", "json_object"));
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            log.info("Sending request to OpenAI API: {}", apiUrl);
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            log.info("OpenAI API response status: {}", response.getStatusCode());
            
            if (responseBody != null) {
                // Check for errors first
                if (responseBody.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) responseBody.get("error");
                    String errorMessage = error != null ? (String) error.get("message") : "Unknown error";
                    log.error("OpenAI API error: {}", errorMessage);
                    throw new RuntimeException("OpenAI API error: " + errorMessage);
                }
                
                if (responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        String content = (String) message.get("content");
                        log.info("OpenAI response content length: {}", content != null ? content.length() : 0);
                        return content;
                    }
                }
            }
            
            log.error("Invalid OpenAI API response structure: {}", responseBody);
            throw new RuntimeException("Invalid OpenAI API response");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("OpenAI API HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API HTTP error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private String parseOpenAIResponse(String response) {
        // OpenAI should return JSON, but we clean it up if needed
        try {
            // Validate JSON
            objectMapper.readTree(response);
            return response;
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI response as JSON: {}", response);
            // Try to extract JSON from markdown code blocks
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                if (end > start) {
                    return response.substring(start, end).trim();
                }
            }
            return "{}";
        }
    }
}






