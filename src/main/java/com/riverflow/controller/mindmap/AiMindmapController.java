package com.riverflow.controller.mindmap;

import com.riverflow.config.jwt.CustomUserDetailsService;
import com.riverflow.dto.mindmap.MindmapResponse;
import com.riverflow.dto.mindmap.ai.ExpandNodeRequest;
import com.riverflow.dto.mindmap.ai.GenerateMindmapRequest;
import com.riverflow.dto.mindmap.ai.OptimizeRequest;
import com.riverflow.model.User;
import com.riverflow.service.mindmap.ai.AiMindmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AiMindmapController {

    private final AiMindmapService aiMindmapService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping("/generate")
    public ResponseEntity<MindmapResponse> generateMindmap(
            @Valid @RequestBody GenerateMindmapRequest request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        log.info("AI generate mindmap for user: {} topic: {}", userId, request.getTopic());
        MindmapResponse response = aiMindmapService.generateMindmap(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/expand-node")
    public ResponseEntity<MindmapResponse> expandNode(
            @Valid @RequestBody ExpandNodeRequest request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        log.info("AI expand node {} in map {} by user {}", request.getNodeId(), request.getMindmapId(), userId);
        MindmapResponse response = aiMindmapService.expandNode(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/optimize")
    public ResponseEntity<MindmapResponse> optimize(
            @Valid @RequestBody OptimizeRequest request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        log.info("AI optimize {} for map {} by user {}", request.getTargetType(), request.getMindmapId(), userId);
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



