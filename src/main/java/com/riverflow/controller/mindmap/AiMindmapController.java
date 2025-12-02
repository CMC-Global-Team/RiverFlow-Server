package com.riverflow.controller.mindmap;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.ai.GenerateMindmapRequest;
import com.riverflow.dto.mindmap.ai.OptimizeRequest;
import com.riverflow.model.User;
import com.riverflow.service.mindmap.ai.AiMindmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mindmaps/ai")
@RequiredArgsConstructor
public class AiMindmapController {

    private final AiMindmapService aiMindmapService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/generate")
    public ResponseEntity<MindmapResponse> generateMindmap(
            @Valid @RequestBody GenerateMindmapRequest request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        MindmapResponse response = aiMindmapService.generateMindmap(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
//add optimize mindmap request

    @PostMapping("/optimize")
    public ResponseEntity<MindmapResponse> optimize(
            @Valid @RequestBody OptimizeRequest request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        MindmapResponse response = aiMindmapService.optimize(request, userId);
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null) return null;
        String email = authentication.getName();
        User user = userDetailsService.loadUserEntityByEmail(email);
        return user.getId();
    }
}

