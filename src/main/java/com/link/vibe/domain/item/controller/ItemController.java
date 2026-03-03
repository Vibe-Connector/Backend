package com.link.vibe.domain.item.controller;

import com.link.vibe.domain.item.dto.CoffeeDetailResponse;
import com.link.vibe.domain.item.dto.LightingDetailResponse;
import com.link.vibe.domain.item.dto.MovieDetailResponse;
import com.link.vibe.domain.item.dto.MusicDetailResponse;
import com.link.vibe.domain.item.service.ItemService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.i18n.LanguageContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Items", description = "아이템 상세 API — 영화, 음악, 조명, 커피 도메인별 상세 조회 (i18n 지원)")
@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(
            summary = "영화 상세 조회",
            description = """
                    아이템의 공통 정보(이름, 카테고리, 브랜드)와 영화 도메인 상세(TMDB 데이터)를 조회합니다.

                    **인증 불필요:** 공개 API입니다.

                    **i18n:** Accept-Language 헤더(ko, en)에 따라 아이템명·카테고리명이 해당 언어로 반환됩니다.
                    번역 데이터가 없으면 해당 필드는 null로 반환됩니다.

                    **에러:**
                    - 404 (ITEM_001): 아이템이 존재하지 않거나, 비활성 상태이거나, 해당 카테고리의 상세 정보가 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이템을 찾을 수 없음")
    })
    @GetMapping("/{itemId}/movie")
    public ApiResponse<MovieDetailResponse> getMovieDetail(
            @Parameter(description = "아이템 ID", example = "1") @PathVariable Long itemId) {
        Long languageId = LanguageContext.getLanguageId();
        return ApiResponse.ok(itemService.getMovieDetail(itemId, languageId));
    }

    @Operation(
            summary = "음악 상세 조회",
            description = """
                    아이템의 공통 정보(이름, 카테고리, 브랜드)와 음악 도메인 상세(Spotify 데이터)를 조회합니다.

                    **인증 불필요:** 공개 API입니다.

                    **i18n:** Accept-Language 헤더(ko, en)에 따라 아이템명·카테고리명이 해당 언어로 반환됩니다.
                    번역 데이터가 없으면 해당 필드는 null로 반환됩니다.

                    **에러:**
                    - 404 (ITEM_001): 아이템이 존재하지 않거나, 비활성 상태이거나, 해당 카테고리의 상세 정보가 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이템을 찾을 수 없음")
    })
    @GetMapping("/{itemId}/music")
    public ApiResponse<MusicDetailResponse> getMusicDetail(
            @Parameter(description = "아이템 ID", example = "1") @PathVariable Long itemId) {
        Long languageId = LanguageContext.getLanguageId();
        return ApiResponse.ok(itemService.getMusicDetail(itemId, languageId));
    }

    @Operation(
            summary = "조명 상세 조회",
            description = """
                    아이템의 공통 정보(이름, 카테고리, 브랜드)와 조명 도메인 상세를 조회합니다.

                    **인증 불필요:** 공개 API입니다.

                    **i18n:** Accept-Language 헤더(ko, en)에 따라 아이템명·카테고리명이 해당 언어로 반환됩니다.
                    번역 데이터가 없으면 해당 필드는 null로 반환됩니다.

                    **에러:**
                    - 404 (ITEM_001): 아이템이 존재하지 않거나, 비활성 상태이거나, 해당 카테고리의 상세 정보가 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이템을 찾을 수 없음")
    })
    @GetMapping("/{itemId}/lighting")
    public ApiResponse<LightingDetailResponse> getLightingDetail(
            @Parameter(description = "아이템 ID", example = "1") @PathVariable Long itemId) {
        Long languageId = LanguageContext.getLanguageId();
        return ApiResponse.ok(itemService.getLightingDetail(itemId, languageId));
    }

    @Operation(
            summary = "커피 상세 조회",
            description = """
                    아이템의 공통 정보(이름, 카테고리, 브랜드)와 커피 도메인 상세를 조회합니다.

                    **인증 불필요:** 공개 API입니다.

                    **i18n:** Accept-Language 헤더(ko, en)에 따라 아이템명·카테고리명이 해당 언어로 반환됩니다.
                    번역 데이터가 없으면 해당 필드는 null로 반환됩니다.

                    **에러:**
                    - 404 (ITEM_001): 아이템이 존재하지 않거나, 비활성 상태이거나, 해당 카테고리의 상세 정보가 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이템을 찾을 수 없음")
    })
    @GetMapping("/{itemId}/coffee")
    public ApiResponse<CoffeeDetailResponse> getCoffeeDetail(
            @Parameter(description = "아이템 ID", example = "1") @PathVariable Long itemId) {
        Long languageId = LanguageContext.getLanguageId();
        return ApiResponse.ok(itemService.getCoffeeDetail(itemId, languageId));
    }
}
