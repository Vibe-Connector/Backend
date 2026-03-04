package com.link.vibe.domain.explore.controller;

import com.link.vibe.domain.explore.dto.ExplorePeriod;
import com.link.vibe.domain.explore.dto.ExploreVibeResponse;
import com.link.vibe.domain.explore.service.ExploreService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Explore", description = "탐색 API — 인기 Vibe를 기간별로 무한 스크롤 조회")
@RestController
@RequestMapping("/api/v1/explore")
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;

    @Operation(
            summary = "인기 Vibe 탐색",
            description = """
                    기간별 인기 Vibe를 무한 스크롤로 조회합니다.

                    **선택적 인증:** 비인증 시에도 조회 가능하며, 인증 시 isArchived/archiveId가 포함됩니다.

                    **인기도 공식:** engagement×10 + viewCount + engagement×1000/(viewCount+10)
                    engagement = 반응수 + 댓글수 (본인 댓글 제외)

                    **커서 형식:** `{popularityScore}_{feedId}` (예: "190_42")
                    동일 점수 시 feedId 기준 안정 정렬됩니다.

                    **기간 필터:** DAY(24시간), WEEK(7일, 기본값), MONTH(30일)
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/vibes")
    public ApiResponse<PageResponse<ExploreVibeResponse>> getPopularVibes(
            @Parameter(description = "기간 필터 (DAY=24시간, WEEK=7일, MONTH=30일)", example = "WEEK")
            @RequestParam(defaultValue = "WEEK") ExplorePeriod period,
            @ModelAttribute CursorPageRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserIdOrNull();
        return ApiResponse.ok(exploreService.getPopularVibes(period, request, currentUserId));
    }
}
