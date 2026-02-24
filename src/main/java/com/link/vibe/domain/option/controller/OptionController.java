package com.link.vibe.domain.option.controller;

import com.link.vibe.domain.option.dto.OptionResponse;
import com.link.vibe.domain.option.service.OptionService;
import com.link.vibe.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Option", description = "Vibe 선택지 조회 API — v3.2 스키마 기반 (mood_keywords, time_options, weather_options, place_options, companion_options)")
@RestController
@RequestMapping("/api/v1/options")
@RequiredArgsConstructor
public class OptionController {

    private final OptionService optionService;

    @Operation(
            summary = "전체 선택지 조회",
            description = """
                    Vibe 생성에 필요한 모든 선택지를 조회합니다.

                    **응답 구조 (v3.2):**
                    - **moods**: 기분 키워드 목록 (keywordId, keywordValue, category) — mood_keywords 테이블
                    - **times**: 시간대 옵션 (timeId, timeKey, timeValue, period) — time_options 테이블 (is_active=true)
                    - **weathers**: 날씨 옵션 (weatherId, weatherKey) — weather_options 테이블 (is_active=true)
                    - **places**: 공간 옵션 (placeId, placeKey) — place_options 테이블 (is_active=true)
                    - **companions**: 동반자 옵션 (companionId, companionKey) — companion_options 테이블 (is_active=true)

                    *다국어 표시 텍스트는 별도 번역 테이블(_translations)에서 관리됩니다.*
                    """
    )
    @GetMapping
    public ApiResponse<OptionResponse> getAllOptions() {
        return ApiResponse.ok(optionService.getAllOptions());
    }
}
