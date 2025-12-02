package com.riverflow.controller.mindmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.ai.GenerateMindmapRequest;
import com.riverflow.dto.mindmap.ai.OptimizeRequest;
import com.riverflow.model.User;
import com.riverflow.service.mindmap.ai.AiMindmapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for AiMindmapController using MockMvc
 */
@WebMvcTest(controllers = AiMindmapController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class AiMindmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiMindmapService aiMindmapService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private User testUser;
    private MindmapResponse mindmapResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();

        mindmapResponse = new MindmapResponse();
        mindmapResponse.setId("ai-mindmap-123");
        mindmapResponse.setTitle("AI Generated Mindmap");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void generateMindmap_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        GenerateMindmapRequest request = new GenerateMindmapRequest();
        request.setTopic("Artificial Intelligence");
        
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(aiMindmapService.generateMindmap(any(GenerateMindmapRequest.class), eq(1L))).thenReturn(mindmapResponse);

        // When & Then
        mockMvc.perform(post("/mindmaps/ai/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ai-mindmap-123"))
                .andExpect(jsonPath("$.title").value("AI Generated Mindmap"));

        verify(aiMindmapService).generateMindmap(any(GenerateMindmapRequest.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void optimize_ValidRequest_ReturnsOptimized() throws Exception {
        // Given
        OptimizeRequest request = new OptimizeRequest();
        request.setMindmapId("mindmap-123");
        
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(aiMindmapService.optimize(any(OptimizeRequest.class), eq(1L))).thenReturn(mindmapResponse);

        // When & Then
        mockMvc.perform(post("/mindmaps/ai/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ai-mindmap-123"));

        verify(aiMindmapService).optimize(any(OptimizeRequest.class), eq(1L));
    }
}
