package com.link.vibe.domain.vibe.controller;

import com.link.vibe.domain.vibe.dto.*;
import com.link.vibe.domain.vibe.service.VibeService;
import com.link.vibe.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Vibe", description = "Vibe 생성 및 조회 API — vibe_sessions, vibe_prompts, vibe_results 테이블 기반")
@RestController
@RequestMapping("/api/v1/vibes")
@RequiredArgsConstructor
public class VibeController {

    private final VibeService vibeService;

    @Operation(
            summary = "Vibe 생성",
            description = """
                    3단계 선택 결과(기분 키워드, 시간, 날씨, 공간, 동반자)를 기반으로 AI 분위기를 생성합니다.

                    **처리 흐름:**
                    1. vibe_sessions 레코드 생성 (status=IN_PROGRESS)
                    2. vibe_prompts 레코드 생성 (선택 옵션 + 최종 프롬프트)
                    3. OpenAI API 호출하여 분위기 문구 및 분석 생성
                    4. vibe_results 레코드 생성 (AI 결과)
                    5. 세션 상태를 COMPLETED로 갱신
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 옵션 ID")
    })
    @PostMapping
    public ApiResponse<VibeResultResponse> createVibe(@Valid @RequestBody VibeCreateRequest request) {
        return ApiResponse.ok(vibeService.createVibe(request));
    }

    @Operation(
            summary = "Vibe 히스토리 조회",
            description = "사용자의 Vibe 생성 히스토리를 최신순으로 조회합니다. 완료된 세션만 반환됩니다."
    )
    @GetMapping
    public ApiResponse<List<VibeHistoryResponse>> getHistory(
            @Parameter(description = "사용자 ID (기본값: 1)", example = "1")
            @RequestParam(defaultValue = "1") Long userId) {
        return ApiResponse.ok(vibeService.getHistory(userId));
    }

    @Operation(
            summary = "Vibe 상세 조회",
            description = "특정 세션의 Vibe 상세 정보(프롬프트 + 결과)를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @GetMapping("/{sessionId}")
    public ApiResponse<VibeResultResponse> getVibeDetail(
            @Parameter(description = "세션 ID", example = "1")
            @PathVariable Long sessionId) {
        return ApiResponse.ok(vibeService.getVibeDetail(sessionId));
    }
}
