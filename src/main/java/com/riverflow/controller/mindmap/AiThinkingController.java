package com.riverflow.controller.mindmap;

import com.riverflow.dto.mindmap.ai.Otmz;
import com.riverflow.dto.mindmap.ai.ThinkingModeRequest;
import com.riverflow.service.mindmap.ai.AiThinkingModeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/thinking")
@RequiredArgsConstructor
public class AiThinkingController {

    private final AiThinkingModeService thinkingService;

    @PostMapping("/otmz")
    public ResponseEntity<Otmz> think(@Valid @RequestBody ThinkingModeRequest request) {
        Otmz otmz = thinkingService.think(
                request.getTopic(),
                request.getLanguage(),
                request.getStructureType(),
                request.getLevels(),
                request.getFirstLevelCount(),
                request.getTags(),
                request.getMode()
        );
        return ResponseEntity.ok(otmz);
    }
}

