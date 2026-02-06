package com.link.vibe.domain.vibe.controller;

import com.link.vibe.domain.vibe.dto.*;
import com.link.vibe.domain.vibe.service.VibeService;
import com.link.vibe.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Vibe", description = "Vibe 생성 및 조회 API")
@RestController
@RequestMapping("/api/v1/vibes")
@RequiredArgsConstructor
public class VibeController {

    private final VibeService vibeService;

    @Operation(summary = "Vibe 생성", description = "3단계 선택 결과를 기반으로 AI 분위기를 생성합니다.")
    @PostMapping
    public ApiResponse<VibeResultResponse> createVibe(@Valid @RequestBody VibeCreateRequest request) {
        return ApiResponse.ok(vibeService.createVibe(request));
    }

    @Operation(summary = "Vibe 히스토리 조회", description = "세션별 Vibe 생성 히스토리를 조회합니다.")
    @GetMapping
    public ApiResponse<List<VibeHistoryResponse>> getHistory(@RequestParam String sessionId) {
        return ApiResponse.ok(vibeService.getHistory(sessionId));
    }

    @Operation(summary = "Vibe 상세 조회", description = "특정 Vibe의 상세 정보를 조회합니다.")
    @GetMapping("/{vibeId}")
    public ApiResponse<VibeResultResponse> getVibeDetail(@PathVariable Long vibeId) {
        return ApiResponse.ok(vibeService.getVibeDetail(vibeId));
    }
}
