package com.riverflow.controller.mindmap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.mindmap.CreateMindmapRequest;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.MindmapSummaryResponse;
import com.riverflow.dto.mindmap.UpdateMindmapRequest;
import com.riverflow.model.User;
import com.riverflow.repository.mindmap.MindmapRepository;
import com.riverflow.service.mindmap.CollaborationService;
import com.riverflow.service.mindmap.MindmapService;
import com.riverflow.service.mindmap.UndoRedoService;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for MindmapController using MockMvc
 */
@WebMvcTest(controllers = MindmapController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class MindmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MindmapService mindmapService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UndoRedoService undoRedoService;

    @MockBean
    private CollaborationService collaborationService;

    @MockBean
    private MindmapRepository mindmapRepository;

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
        mindmapResponse.setId("mindmap-123");
        mindmapResponse.setTitle("Test Mindmap");
        mindmapResponse.setOwnerId(1L);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createMindmap_ValidRequest_ReturnsCreated() throws Exception {
        // Given
        CreateMindmapRequest request = new CreateMindmapRequest();
        request.setTitle("Test Mindmap");
        
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(mindmapService.createMindmap(any(CreateMindmapRequest.class), eq(1L))).thenReturn(mindmapResponse);

        // When & Then
        mockMvc.perform(post("/mindmaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("mindmap-123"))
                .andExpect(jsonPath("$.title").value("Test Mindmap"));

        verify(mindmapService).createMindmap(any(CreateMindmapRequest.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getAllMindmaps_AuthenticatedUser_ReturnsList() throws Exception {
        // Given
        MindmapSummaryResponse summary = new MindmapSummaryResponse();
        summary.setId("mindmap-123");
        summary.setTitle("Test Mindmap");
        List<MindmapSummaryResponse> list = Arrays.asList(summary);

        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(mindmapService.getAllMindmapsByUser(1L)).thenReturn(list);

        // When & Then
        mockMvc.perform(get("/mindmaps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("mindmap-123"))
                .andExpect(jsonPath("$[0].title").value("Test Mindmap"));

        verify(mindmapService).getAllMindmapsByUser(1L);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getMindmapById_AuthenticatedUser_ReturnsMindmap() throws Exception {
        // Given
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(mindmapService.getMindmapById("mindmap-123", 1L)).thenReturn(mindmapResponse);

        // When & Then
        mockMvc.perform(get("/mindmaps/mindmap-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mindmap-123"));

        verify(mindmapService).getMindmapById("mindmap-123", 1L);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updateMindmap_ValidRequest_ReturnsUpdated() throws Exception {
        // Given
        UpdateMindmapRequest request = new UpdateMindmapRequest();
        request.setTitle("Updated Title");

        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(mindmapService.updateMindmap(eq("mindmap-123"), any(UpdateMindmapRequest.class), eq(1L)))
                .thenReturn(mindmapResponse);

        // When & Then
        mockMvc.perform(put("/mindmaps/mindmap-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(mindmapService).updateMindmap(eq("mindmap-123"), any(UpdateMindmapRequest.class), eq(1L));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteMindmap_AuthenticatedUser_ReturnsSuccess() throws Exception {
        // Given
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        doNothing().when(mindmapService).deleteMindmap("mindmap-123", 1L);

        // When & Then
        mockMvc.perform(delete("/mindmaps/mindmap-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mindmap deleted successfully"));

        verify(mindmapService).deleteMindmap("mindmap-123", 1L);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void duplicateMindmap_AuthenticatedUser_ReturnsDuplicated() throws Exception {
        // Given
        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(mindmapService.duplicateMindmap("mindmap-123", 1L)).thenReturn(mindmapResponse);

        // When & Then
        mockMvc.perform(post("/mindmaps/mindmap-123/duplicate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("mindmap-123"));

        verify(mindmapService).duplicateMindmap("mindmap-123", 1L);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void searchMindmaps_AuthenticatedUser_ReturnsResults() throws Exception {
        // Given
        MindmapSummaryResponse summary = new MindmapSummaryResponse();
        summary.setId("mindmap-123");
        List<MindmapSummaryResponse> list = Arrays.asList(summary);

        when(userDetailsService.loadUserEntityByEmail("test@example.com")).thenReturn(testUser);
        when(mindmapService.searchMindmaps(1L, "keyword")).thenReturn(list);

        // When & Then
        mockMvc.perform(get("/mindmaps/search")
                        .param("keyword", "keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("mindmap-123"));

        verify(mindmapService).searchMindmaps(1L, "keyword");
    }
}
