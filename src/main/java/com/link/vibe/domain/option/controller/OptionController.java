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

@Tag(name = "Option", description = "선택지 조회 API")
@RestController
@RequestMapping("/api/v1/options")
@RequiredArgsConstructor
public class OptionController {

    private final OptionService optionService;

    @Operation(summary = "전체 선택지 조회", description = "기분, 시간, 날씨, 공간, 동반자 선택지를 모두 조회합니다.")
    @GetMapping
    public ApiResponse<OptionResponse> getAllOptions() {
        return ApiResponse.ok(optionService.getAllOptions());
    }
}
