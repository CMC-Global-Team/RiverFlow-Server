package com.riverflow.service.mindmap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.dto.mindmap.AiMindmapRequest;
import com.riverflow.dto.mindmap.AiMindmapResponse;
import com.riverflow.dto.mindmap.GeneratedMindmapNode;
import com.riverflow.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementation for AI Mindmap Assistant operations
 * 
 * Supports two modes:
 * 1. LLM mode: Uses actual LLM service (OpenAI, Claude, etc.) if configured
 * 2. Fallback mode: Uses rule-based logic if LLM is not available
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiMindmapServiceImpl implements AiMindmapService {
    
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private ChatClient chatClient;
    
    private static final String DEFAULT_COLOR = "#3b82f6";
    private static final String DEFAULT_EDGE_COLOR = "#ef4444";
    private static final int DEFAULT_NODE_WIDTH = 150;
    private static final int DEFAULT_NODE_HEIGHT = 48;
    
    @Override
    public AiMindmapResponse processAiRequest(AiMindmapRequest request) {
        log.info("Processing AI mindmap request: {}", request.getUserInstruction());
        log.info("LLM Service instance: {}", llmService != null ? llmService.getClass().getName() : "NULL");
        
        // Try LLM mode first if available
        log.info("Checking LLM service availability...");
        boolean llmAvailable = llmService != null && llmService.isAvailable();
        log.info("LLM service available: {}", llmAvailable);
        
        if (llmAvailable) {
            log.info("LLM service is available, using AI generation");
            try {
                String jsonResponse = llmService.generateMindmapContent(request);
                log.info("LLM response: {}", jsonResponse);
                if (jsonResponse != null && !jsonResponse.equals("{}") && !jsonResponse.trim().isEmpty()) {
                    AiMindmapResponse response = parseLlmResponse(jsonResponse);
                    if (response.getNodes() != null && !response.getNodes().isEmpty()) {
                        log.info("Successfully generated {} nodes using LLM", response.getNodes().size());
                        return response;
                    } else {
                        log.warn("LLM returned empty nodes, falling back to rule-based logic");
                    }
                } else {
                    log.warn("LLM returned empty response, falling back to rule-based logic");
                }
            } catch (Exception e) {
                log.warn("LLM service failed, falling back to rule-based logic: {}", e.getMessage(), e);
                // Fall through to rule-based logic
            }
        } else {
            log.info("LLM service not available, using rule-based logic");
        }
        
        // Fallback to rule-based logic
        String instruction = request.getUserInstruction() != null 
            ? request.getUserInstruction().toLowerCase() 
            : "";
        
        // Determine action type based on instruction
        if (instruction.contains("expand") || instruction.contains("mở rộng")) {
            return expandNode(request);
        } else if (instruction.contains("summarize") || instruction.contains("tóm tắt")) {
            return summarizeNode(request);
        } else if (instruction.contains("add") || instruction.contains("thêm")) {
            return addNewNodes(request);
        } else if (instruction.contains("restructure") || instruction.contains("tái cấu trúc")) {
            return restructureMindmap(request);
        } else {
            // Default: expand node
            return expandNode(request);
        }
    }
    
    /**
     * Parse LLM JSON response into AiMindmapResponse
     */
    private AiMindmapResponse parseLlmResponse(String jsonResponse) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(
                jsonResponse, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) responseMap.get("nodes");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> edges = (List<Map<String, Object>>) responseMap.get("edges");
            
            return AiMindmapResponse.builder()
                .nodes(nodes != null ? nodes : new ArrayList<>())
                .edges(edges != null ? edges : new ArrayList<>())
                .build();
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid LLM response format", e);
        }
    }
    
    /**
     * Expand a node by adding 2-4 child nodes
     */
    private AiMindmapResponse expandNode(AiMindmapRequest request) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        String nodeId = request.getNodeTitle() != null 
            ? generateNodeId(request.getNodeTitle()) 
            : "1";
        
        // Add parent node if nodeTitle exists
        if (request.getNodeTitle() != null && !request.getNodeTitle().isEmpty()) {
            Map<String, Object> parentNode = createNode(
                nodeId,
                request.getNodeTitle(),
                getCenterPosition()
            );
            nodes.add(parentNode);
        }
        
        // Generate 2-4 child nodes
        int childCount = 2 + (int)(Math.random() * 3); // 2-4 nodes
        String[] childLabels = generateChildLabels(request);
        
        for (int i = 0; i < Math.min(childCount, childLabels.length); i++) {
            String childId = generateNodeId(childLabels[i] + i);
            Map<String, Object> childNode = createNode(
                childId,
                childLabels[i],
                calculateChildPosition(getCenterPosition(), i, childCount)
            );
            nodes.add(childNode);
            
            // Create edge from parent to child
            if (request.getNodeTitle() != null && !request.getNodeTitle().isEmpty()) {
                Map<String, Object> edge = createEdge(nodeId, childId, i == 0);
                edges.add(edge);
            }
        }
        
        return AiMindmapResponse.builder()
            .nodes(nodes)
            .edges(edges)
            .build();
    }
    
    /**
     * Summarize node - returns simplified structure
     */
    private AiMindmapResponse summarizeNode(AiMindmapRequest request) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        // Create summary node
        String summaryId = "summary-1";
        String summaryText = request.getNodeSummary() != null 
            ? request.getNodeSummary() 
            : (request.getNodeTitle() != null ? "Summary of " + request.getNodeTitle() : "Summary");
        
        Map<String, Object> summaryNode = createNode(
            summaryId,
            summaryText,
            getCenterPosition()
        );
        nodes.add(summaryNode);
        
        // Add key points as children if context exists
        if (request.getContextNodes() != null && !request.getContextNodes().isEmpty()) {
            int keyPointCount = Math.min(3, request.getContextNodes().size());
            for (int i = 0; i < keyPointCount; i++) {
                com.riverflow.dto.mindmap.ContextNode contextNode = request.getContextNodes().get(i);
                String keyPointId = "keypoint-" + i;
                String keyPointText = contextNode.getSummary() != null 
                    ? contextNode.getSummary() 
                    : "Key Point " + (i + 1);
                
                Map<String, Object> keyPointNode = createNode(
                    keyPointId,
                    keyPointText,
                    calculateChildPosition(getCenterPosition(), i, keyPointCount)
                );
                nodes.add(keyPointNode);
                
                Map<String, Object> edge = createEdge(summaryId, keyPointId, false);
                edges.add(edge);
            }
        }
        
        return AiMindmapResponse.builder()
            .nodes(nodes)
            .edges(edges)
            .build();
    }
    
    /**
     * Add new nodes based on user instruction
     */
    private AiMindmapResponse addNewNodes(AiMindmapRequest request) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        // Parse new ideas from instruction
        String[] newIdeas = extractNewIdeas(request.getUserInstruction());
        
        String parentId = request.getNodeTitle() != null 
            ? generateNodeId(request.getNodeTitle()) 
            : "root";
        
        // Add parent node
        if (request.getNodeTitle() != null && !request.getNodeTitle().isEmpty()) {
            Map<String, Object> parentNode = createNode(
                parentId,
                request.getNodeTitle(),
                getCenterPosition()
            );
            nodes.add(parentNode);
        }
        
        // Add new nodes
        for (int i = 0; i < newIdeas.length; i++) {
            String newNodeId = generateNodeId(newIdeas[i] + i);
            Map<String, Object> newNode = createNode(
                newNodeId,
                newIdeas[i],
                calculateChildPosition(getCenterPosition(), i, newIdeas.length)
            );
            nodes.add(newNode);
            
            // Create edge
            if (request.getNodeTitle() != null && !request.getNodeTitle().isEmpty()) {
                Map<String, Object> edge = createEdge(parentId, newNodeId, i == 0);
                edges.add(edge);
            }
        }
        
        return AiMindmapResponse.builder()
            .nodes(nodes)
            .edges(edges)
            .build();
    }
    
    /**
     * Restructure mindmap
     */
    private AiMindmapResponse restructureMindmap(AiMindmapRequest request) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        // Create main node
        String mainId = "main-1";
        String mainLabel = request.getNodeTitle() != null 
            ? request.getNodeTitle() 
            : "Main Topic";
        
        Map<String, Object> mainNode = createNode(mainId, mainLabel, getCenterPosition());
        nodes.add(mainNode);
        
        // Restructure context nodes into logical groups
        if (request.getContextNodes() != null && !request.getContextNodes().isEmpty()) {
            int groupCount = Math.min(3, (request.getContextNodes().size() + 1) / 2);
            int nodesPerGroup = (int) Math.ceil((double) request.getContextNodes().size() / groupCount);
            
            for (int group = 0; group < groupCount; group++) {
                String groupId = "group-" + group;
                String groupLabel = "Group " + (group + 1);
                
                Map<String, Object> groupNode = createNode(
                    groupId,
                    groupLabel,
                    calculateChildPosition(getCenterPosition(), group, groupCount)
                );
                nodes.add(groupNode);
                
                // Link main to group
                Map<String, Object> mainEdge = createEdge(mainId, groupId, group == 0);
                edges.add(mainEdge);
                
                // Add context nodes to group
                int startIdx = group * nodesPerGroup;
                int endIdx = Math.min(startIdx + nodesPerGroup, request.getContextNodes().size());
                
                for (int i = startIdx; i < endIdx; i++) {
                    com.riverflow.dto.mindmap.ContextNode contextNode = request.getContextNodes().get(i);
                    String childId = "node-" + i;
                    String childLabel = contextNode.getSummary() != null 
                        ? contextNode.getSummary() 
                        : "Node " + (i + 1);
                    
                    Map<String, Object> childNode = createNode(
                        childId,
                        childLabel,
                        calculateChildPosition(
                            calculateChildPosition(getCenterPosition(), group, groupCount),
                            i - startIdx,
                            endIdx - startIdx
                        )
                    );
                    nodes.add(childNode);
                    
                    Map<String, Object> childEdge = createEdge(groupId, childId, false);
                    edges.add(childEdge);
                }
            }
        }
        
        return AiMindmapResponse.builder()
            .nodes(nodes)
            .edges(edges)
            .build();
    }
    
    /**
     * Create a node in React Flow format
     */
    private Map<String, Object> createNode(String id, String label, Map<String, Double> position) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", id);
        node.put("type", "rectangle");
        node.put("position", position);
        
        Map<String, Object> data = new HashMap<>();
        data.put("label", label);
        data.put("description", "");
        data.put("color", DEFAULT_COLOR);
        data.put("shape", "rectangle");
        node.put("data", data);
        
        node.put("width", DEFAULT_NODE_WIDTH);
        node.put("height", DEFAULT_NODE_HEIGHT);
        node.put("selected", false);
        
        Map<String, Double> positionAbsolute = new HashMap<>(position);
        node.put("positionAbsolute", positionAbsolute);
        node.put("dragging", false);
        
        return node;
    }
    
    /**
     * Create an edge in React Flow format
     */
    private Map<String, Object> createEdge(String source, String target, boolean animated) {
        Map<String, Object> edge = new HashMap<>();
        edge.put("id", "e" + source + "-" + target);
        edge.put("source", source);
        edge.put("target", target);
        edge.put("animated", animated);
        edge.put("type", "smoothstep");
        edge.put("selected", false);
        
        if (animated) {
            Map<String, String> style = new HashMap<>();
            style.put("stroke", DEFAULT_EDGE_COLOR);
            edge.put("style", style);
            
            Map<String, String> markerEnd = new HashMap<>();
            markerEnd.put("type", "arrowclosed");
            markerEnd.put("color", DEFAULT_EDGE_COLOR);
            edge.put("markerEnd", markerEnd);
        }
        
        return edge;
    }
    
    /**
     * Generate child labels based on node title and context
     */
    private String[] generateChildLabels(AiMindmapRequest request) {
        String baseTitle = request.getNodeTitle() != null 
            ? request.getNodeTitle() 
            : "Topic";
        
        // Generate context-aware labels
        String[] defaultLabels = {
            baseTitle + " - Aspect 1",
            baseTitle + " - Aspect 2",
            baseTitle + " - Detail 1",
            baseTitle + " - Detail 2"
        };
        
        // If context nodes exist, use their summaries
        if (request.getContextNodes() != null && !request.getContextNodes().isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (com.riverflow.dto.mindmap.ContextNode contextNode : request.getContextNodes()) {
                if (labels.size() >= 4) break;
                String summary = contextNode.getSummary();
                if (summary != null && !summary.isEmpty()) {
                    labels.add(summary.length() > 30 ? summary.substring(0, 30) + "..." : summary);
                }
            }
            if (!labels.isEmpty()) {
                return labels.toArray(new String[0]);
            }
        }
        
        return defaultLabels;
    }
    
    /**
     * Extract new ideas from user instruction
     */
    private String[] extractNewIdeas(String instruction) {
        if (instruction == null || instruction.isEmpty()) {
            return new String[]{"New Idea 1", "New Idea 2"};
        }
        
        // Simple extraction - split by common separators
        String[] ideas = instruction.split("[,;]|và|and");
        List<String> result = new ArrayList<>();
        
        for (String idea : ideas) {
            idea = idea.trim();
            if (idea.length() > 3 && idea.length() < 50) {
                result.add(idea);
            }
            if (result.size() >= 4) break;
        }
        
        if (result.isEmpty()) {
            return new String[]{"New Idea 1", "New Idea 2"};
        }
        
        return result.toArray(new String[0]);
    }
    
    /**
     * Get center position for root node
     */
    private Map<String, Double> getCenterPosition() {
        Map<String, Double> position = new HashMap<>();
        position.put("x", 280.8799573835257);
        position.put("y", 119.12004261647431);
        return position;
    }
    
    /**
     * Calculate child position based on parent and index
     */
    private Map<String, Double> calculateChildPosition(
            Map<String, Double> parentPos, int index, int total) {
        Map<String, Double> position = new HashMap<>();
        
        double baseX = parentPos.get("x");
        double baseY = parentPos.get("y");
        
        // Arrange children in a row or arc
        double spacing = 200.0;
        double startX = baseX - (total - 1) * spacing / 2;
        
        position.put("x", startX + index * spacing);
        position.put("y", baseY + 150.0); // Below parent
        
        return position;
    }
    
    /**
     * Generate node ID from text
     */
    private String generateNodeId(String text) {
        if (text == null || text.isEmpty()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        return text.toLowerCase()
            .replaceAll("[^a-z0-9]", "-")
            .substring(0, Math.min(20, text.length())) + "-" + 
            UUID.randomUUID().toString().substring(0, 4);
    }
    
    /**
     * Generate mindmap nodes using Spring AI ChatClient
     * 
     * @param request AI mindmap request with node context and user instruction
     * @return List of generated mindmap nodes
     */
    @Override
    public List<GeneratedMindmapNode> generateMindmapNodes(AiMindmapRequest request) {
        log.info("Generating mindmap nodes using Spring AI for instruction: {}", request.getUserInstruction());
        
        if (chatClient == null) {
            log.warn("Spring AI ChatClient is not available. Please configure OpenAI API key.");
            return Collections.emptyList();
        }
        
        try {
            // Build prompt
            String prompt = buildMindmapPrompt(request);
            log.debug("Generated prompt: {}", prompt);
            
            // Call Spring AI ChatClient
            Prompt aiPrompt = new Prompt(new UserMessage(prompt));
            String response = chatClient.call(aiPrompt).getResult().getOutput().getContent();
            log.info("Spring AI response received, length: {}", response != null ? response.length() : 0);
            
            // Parse JSON response
            List<GeneratedMindmapNode> nodes = parseGeneratedNodes(response);
            log.info("Successfully generated {} nodes", nodes.size());
            
            return nodes;
        } catch (Exception e) {
            log.error("Error generating mindmap nodes with Spring AI: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Build prompt for AI to generate mindmap nodes
     */
    private String buildMindmapPrompt(AiMindmapRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là hệ thống sinh sơ đồ tư duy (Mindmap AI).\n");
        prompt.append("Dựa vào dữ liệu JSON đầu vào, hãy mở rộng hoặc tạo thêm các node con phù hợp với chủ đề.\n\n");
        
        prompt.append("Dữ liệu đầu vào:\n");
        if (request.getNodeTitle() != null && !request.getNodeTitle().isEmpty()) {
            prompt.append("- Node hiện tại: ").append(request.getNodeTitle()).append("\n");
        }
        if (request.getNodeSummary() != null && !request.getNodeSummary().isEmpty()) {
            prompt.append("- Tóm tắt: ").append(request.getNodeSummary()).append("\n");
        }
        if (request.getContextNodes() != null && !request.getContextNodes().isEmpty()) {
            prompt.append("- Context nodes:\n");
            for (com.riverflow.dto.mindmap.ContextNode ctx : request.getContextNodes()) {
                prompt.append("  * ID: ").append(ctx.getId()).append(", Summary: ").append(ctx.getSummary()).append("\n");
            }
        }
        if (request.getUserInstruction() != null && !request.getUserInstruction().isEmpty()) {
            prompt.append("- Yêu cầu: ").append(request.getUserInstruction()).append("\n");
        }
        
        prompt.append("\n");
        prompt.append("Đầu ra phải ở dạng JSON hợp lệ, là một mảng các object với các trường:\n");
        prompt.append("- node_title: tiêu đề của node mới (bắt buộc)\n");
        prompt.append("- node_summary: mô tả ngắn gọn của node (bắt buộc)\n");
        prompt.append("- parent_id: id node cha (tùy chọn, chỉ có nếu node có node cha)\n\n");
        
        prompt.append("Ví dụ định dạng JSON đầu ra:\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"node_title\": \"Java Core\",\n");
        prompt.append("    \"node_summary\": \"Các khái niệm cơ bản về Java\",\n");
        prompt.append("    \"parent_id\": \"node-1\"\n");
        prompt.append("  },\n");
        prompt.append("  {\n");
        prompt.append("    \"node_title\": \"Spring Framework\",\n");
        prompt.append("    \"node_summary\": \"Framework phổ biến cho Java\",\n");
        prompt.append("    \"parent_id\": \"node-1\"\n");
        prompt.append("  }\n");
        prompt.append("]\n\n");
        
        prompt.append("Lưu ý:\n");
        prompt.append("- Nếu yêu cầu là 'mở rộng' hoặc 'expand': tạo 2-4 node con phù hợp\n");
        prompt.append("- Nếu yêu cầu là 'tóm tắt' hoặc 'summarize': tạo các node tóm tắt ngắn gọn\n");
        prompt.append("- Nếu yêu cầu là 'thêm' hoặc 'add': thêm các node mới dựa trên instruction\n");
        prompt.append("- Mỗi node_title nên ngắn gọn (≤30 ký tự)\n");
        prompt.append("- Chỉ trả về JSON, không có text hoặc markdown khác\n");
        prompt.append("- Đảm bảo JSON hợp lệ và có thể parse được\n");
        
        return prompt.toString();
    }
    
    /**
     * Parse AI response JSON into list of GeneratedMindmapNode
     */
    private List<GeneratedMindmapNode> parseGeneratedNodes(String jsonResponse) {
        try {
            // Clean response - remove markdown code blocks if present
            String cleanedResponse = jsonResponse.trim();
            if (cleanedResponse.contains("```json")) {
                int start = cleanedResponse.indexOf("```json") + 7;
                int end = cleanedResponse.indexOf("```", start);
                if (end > start) {
                    cleanedResponse = cleanedResponse.substring(start, end).trim();
                }
            } else if (cleanedResponse.contains("```")) {
                int start = cleanedResponse.indexOf("```") + 3;
                int end = cleanedResponse.lastIndexOf("```");
                if (end > start) {
                    cleanedResponse = cleanedResponse.substring(start, end).trim();
                }
            }
            
            // Parse JSON
            List<Map<String, Object>> nodeList = objectMapper.readValue(
                cleanedResponse,
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            // Convert to GeneratedMindmapNode list
            List<GeneratedMindmapNode> nodes = new ArrayList<>();
            for (Map<String, Object> nodeMap : nodeList) {
                GeneratedMindmapNode node = GeneratedMindmapNode.builder()
                    .nodeTitle((String) nodeMap.get("node_title"))
                    .nodeSummary((String) nodeMap.get("node_summary"))
                    .parentId((String) nodeMap.get("parent_id"))
                    .build();
                nodes.add(node);
            }
            
            return nodes;
        } catch (Exception e) {
            log.error("Failed to parse AI response as JSON: {}", jsonResponse, e);
            // Try to extract JSON from response if it's wrapped in text
            try {
                // Look for JSON array pattern
                int start = jsonResponse.indexOf('[');
                int end = jsonResponse.lastIndexOf(']') + 1;
                if (start >= 0 && end > start) {
                    String jsonArray = jsonResponse.substring(start, end);
                    return parseGeneratedNodes(jsonArray);
                }
            } catch (Exception e2) {
                log.error("Failed to extract JSON from response", e2);
            }
            return Collections.emptyList();
        }
    }
}

