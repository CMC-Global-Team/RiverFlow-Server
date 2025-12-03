package com.riverflow.controller.mindmap;

import com.riverflow.dto.mindmap.ai.ActionList;
import com.riverflow.dto.mindmap.ai.Otmz;
import com.riverflow.dto.mindmap.ai.ThinkingModeRequest;
import com.riverflow.service.mindmap.ai.AiThinkingModeService;
import com.riverflow.service.mindmap.ai.GeminiPromptBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/thinking")
@RequiredArgsConstructor
public class AiThinkingController {

    private final AiThinkingModeService thinkingService;
    private final GeminiPromptBuilder promptBuilder;

    @PostMapping("/otmz")
    public ResponseEntity<Otmz> think(@Valid @RequestBody ThinkingModeRequest request,
                                      @RequestParam(required = false) String mindmapId) {
        Otmz otmz = thinkingService.think(
                request.getTopic(),
                request.getLanguage(),
                request.getStructureType(),
                request.getLevels(),
                request.getFirstLevelCount(),
                request.getTags(),
                request.getMode(),
                mindmapId
        );
        return ResponseEntity.ok(otmz);
    }

    // Debug: return the built prompt payload (no AI call)
    @GetMapping("/otmz/prompt")
    public ResponseEntity<Map<String, Object>> getOtmzPrompt(
            @RequestParam String topic,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String structureType,
            @RequestParam(required = false) Integer levels,
            @RequestParam(required = false) Integer firstLevelCount,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String mode
    ) {
        Map<String, Object> payload = promptBuilder.buildThinkingOtmzPrompt(
                topic, language, structureType, levels, firstLevelCount, tags, mode
        );
        return ResponseEntity.ok(payload);
    }

    // Plan: from OTMZ to ActionList
    @PostMapping("/actions")
    public ResponseEntity<ActionList> plan(@RequestBody Otmz otmz,
                                           @RequestParam(required = false) String language,
                                           @RequestParam(required = false) String mindmapId) {
        String lang = language;
        if (lang == null && otmz != null && otmz.getMeta() != null) {
            Object metaLang = otmz.getMeta().get("language");
            if (metaLang != null) lang = String.valueOf(metaLang);
        }
        ActionList actions = thinkingService.plan(otmz, lang, mindmapId);
        return ResponseEntity.ok(actions);
    }
}
