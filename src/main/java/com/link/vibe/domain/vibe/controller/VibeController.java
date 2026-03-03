package com.link.vibe.domain.vibe.controller;

import com.link.vibe.domain.vibe.dto.*;
import com.link.vibe.domain.vibe.service.VibePromptService;
import com.link.vibe.domain.vibe.service.VibeService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    private final VibePromptService vibePromptService;

    @Operation(
            summary = "Vibe 세션 생성",
            description = """
                    새로운 Vibe 세션을 생성합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    세션은 `IN_PROGRESS` 상태로 생성되며,
                    이후 프롬프트 제출(`POST /sessions/{sessionId}/prompt`)을 통해 AI 큐레이션을 진행합니다.
                    """
    )
    @PostMapping("/sessions")
    public ApiResponse<VibeSessionCreateResponse> createSession() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(vibeService.createSession(userId));
    }

    @Operation(
            summary = "Vibe 생성 (통합)",
            description = """
                    3단계 선택 결과(기분 키워드, 시간, 날씨, 공간, 동반자)를 기반으로 AI 분위기를 생성합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

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
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(vibeService.createVibe(userId, request));
    }

    @Operation(
            summary = "Vibe 히스토리 조회",
            description = """
                    현재 로그인된 사용자의 Vibe 생성 히스토리를 최신순으로 조회합니다.
                    완료된 세션만 반환됩니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.
                    """
    )
    @GetMapping("/history")
    public ApiResponse<List<VibeHistoryResponse>> getHistory() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(vibeService.getHistory(userId));
    }

    @Operation(
            summary = "Vibe 프롬프트 제출",
            description = """
                    세션에 프롬프트를 제출하여 AI 분위기 큐레이션 + 아이템 추천을 실행합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **처리 흐름:**
                    1. 세션 상태 검증 (IN_PROGRESS만 허용)
                    2. 선택 옵션 검증 + vibe_prompts 생성
                    3. OpenAI 임베딩 생성 + pgvector 유사도 검색
                    4. OpenAI Chat API 호출 (분위기 문구 + 분석)
                    5. vibe_results + vibe_items 저장
                    6. 세션 상태 COMPLETED로 갱신
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 옵션 ID 또는 세션이 이미 완료됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @PostMapping("/sessions/{sessionId}/prompt")
    public ApiResponse<VibePromptSubmitResponse> submitPrompt(
            @Parameter(description = "세션 ID", example = "1")
            @PathVariable Long sessionId,
            @Valid @RequestBody VibeCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(vibePromptService.submitPrompt(userId, sessionId, request));
    }

    @Operation(
            summary = "Vibe 추천 아이템 조회",
            description = """
                    특정 Vibe 결과의 추천 아이템을 카테고리별로 그룹화하여 조회합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    응답은 카테고리(movie, music, lighting, coffee)별로 추천 아이템 목록을 포함합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결과를 찾을 수 없음")
    })
    @GetMapping("/results/{resultId}/items")
    public ApiResponse<List<CategoryRecommendation>> getVibeItems(
            @Parameter(description = "결과 ID", example = "1")
            @PathVariable Long resultId) {
        return ApiResponse.ok(vibeService.getVibeItems(resultId));
    }

    @Operation(
            summary = "Vibe 상세 조회",
            description = """
                    특정 세션의 Vibe 상세 정보(프롬프트 + 결과)를 조회합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **에러:**
                    - 404 (VIBE_001): 세션을 찾을 수 없음
                    - 404 (VIBE_002): 결과를 찾을 수 없음
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<VibeResultResponse> getVibeDetail(
            @Parameter(description = "세션 ID", example = "1")
            @PathVariable Long sessionId) {
        return ApiResponse.ok(vibeService.getVibeDetail(sessionId));
    }
}
