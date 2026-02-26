package com.link.vibe.domain.item.controller;

import com.link.vibe.domain.item.dto.MovieDetailResponse;
import com.link.vibe.domain.item.dto.MusicDetailResponse;
import com.link.vibe.domain.item.service.ItemService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.i18n.LanguageContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Items", description = "아이템 상세 API")
@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "영화 상세 조회", description = "아이템 공통 정보 + 영화 도메인 상세")
    @GetMapping("/{itemId}/movie")
    public ApiResponse<MovieDetailResponse> getMovieDetail(
            @PathVariable Long itemId) {
        Long languageId = LanguageContext.getLanguageId();
        return ApiResponse.ok(itemService.getMovieDetail(itemId, languageId));
    }

    @Operation(summary = "음악 상세 조회", description = "아이템 공통 정보 + 음악 도메인 상세")
    @GetMapping("/{itemId}/music")
    public ApiResponse<MusicDetailResponse> getMusicDetail(
            @PathVariable Long itemId) {
        Long languageId = LanguageContext.getLanguageId();
        return ApiResponse.ok(itemService.getMusicDetail(itemId, languageId));
    }
}
