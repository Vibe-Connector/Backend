package com.link.vibe.domain.option.controller;

import com.link.vibe.domain.option.dto.OptionResponse;
import com.link.vibe.domain.option.dto.OptionResponse.MoodDto;
import com.link.vibe.domain.option.service.OptionService;
import com.link.vibe.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Option", description = "Vibe 선택지 조회 API — i18n 지원")
@RestController
@RequestMapping("/api/v1/options")
@RequiredArgsConstructor
public class OptionController {

    private final OptionService optionService;

    @Operation(
            summary = "전체 선택지 조회 (i18n)",
            description = """
                    Vibe 생성에 필요한 모든 선택지를 조회합니다.

                    **lang 파라미터:**
                    - `ko` (기본값): 한국어
                    - `en`: 영어
                    - 지원하지 않는 언어 코드 → 400 (LANG_001)

                    **응답 구조:**
                    - **moods**: 기분 키워드 (keywordId, keywordValue, category, label)
                    - **times**: 시간대 옵션 (timeId, timeKey, timeValue, period)
                    - **weathers**: 날씨 옵션 (weatherId, weatherKey, label)
                    - **places**: 공간 옵션 (placeId, placeKey, label)
                    - **companions**: 동반자 옵션 (companionId, companionKey, label)

                    `label`은 해당 언어로 번역된 표시 텍스트입니다.
                    번역이 없는 경우 시스템 키(key)가 반환됩니다.
                    """
    )
    @GetMapping
    public ApiResponse<OptionResponse> getAllOptions(
            @Parameter(description = "언어 코드 (ko, en 등)", example = "ko")
            @RequestParam(defaultValue = "ko") String lang) {
        return ApiResponse.ok(optionService.getAllOptions(lang));
    }

    @Operation(
            summary = "무드 키워드 조회 (카테고리별 필터링)",
            description = """
                    무드 키워드를 조회합니다. category 파라미터로 카테고리별 필터링이 가능합니다.

                    **파라미터:**
                    - `lang`: 언어 코드 (기본값 `ko`)
                    - `category` (선택): 카테고리 필터 (예: `감정`, `분위기`, `에너지`, `색감`)
                      - 미지정 시 전체 키워드 반환
                    """
    )
    @GetMapping("/moods")
    public ApiResponse<List<MoodDto>> getMoodKeywords(
            @Parameter(description = "언어 코드", example = "ko")
            @RequestParam(defaultValue = "ko") String lang,
            @Parameter(description = "카테고리 필터", example = "감정")
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(optionService.getMoodKeywords(lang, category));
    }
}
