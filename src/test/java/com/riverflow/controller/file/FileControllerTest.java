package com.riverflow.controller.file;

import com.riverflow.service.user.AvatarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for FileController using MockMvc
 */
@WebMvcTest(controllers = FileController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AvatarService avatarService;

    @Test
    void serveAvatar_ExistingUser_ReturnsImage() throws Exception {
        // Given
        AvatarService.AvatarDto avatarDto = new AvatarService.AvatarDto(
                "image/jpeg",
                "test-image-content".getBytes()
        );

        when(avatarService.getAvatar(1L)).thenReturn(Optional.of(avatarDto));

        // When & Then
        mockMvc.perform(get("/api/files/avatars/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes("test-image-content".getBytes()));

        verify(avatarService).getAvatar(1L);
    }

    @Test
    void serveAvatar_UserNotFound_ReturnsNotFound() throws Exception {
        // Given
        when(avatarService.getAvatar(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/files/avatars/999"))
                .andExpect(status().isNotFound());

        verify(avatarService).getAvatar(999L);
    }

    @Test
    void serveAvatar_ServiceException_ReturnsNotFound() throws Exception {
        // Given
        when(avatarService.getAvatar(1L)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/files/avatars/1"))
                .andExpect(status().isNotFound());

        verify(avatarService).getAvatar(1L);
    }
}
