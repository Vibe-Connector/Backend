package com.link.vibe.domain.feed.controller;

import com.link.vibe.domain.feed.dto.*;
import com.link.vibe.domain.feed.entity.ReactionType;
import com.link.vibe.domain.feed.service.FeedService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feeds", description = "피드 API — 피드 CRUD, 반응 토글, 댓글/대댓글, 좋아요")
@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    // ── 피드 CRUD ──

    @Operation(
            summary = "피드 작성",
            description = """
                    Vibe 결과를 피드로 공유합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    동일한 Vibe 결과로 중복 피드를 생성할 수 없습니다.
                    isPublic을 false로 설정하면 본인만 볼 수 있는 비공개 피드가 됩니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 404 (VIBE_002): Vibe 결과를 찾을 수 없음
                    - 409 (FEED_002): 이미 존재하는 피드
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vibe 결과를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동일 결과로 이미 피드 생성됨")
    })
    @PostMapping("/api/v1/feeds")
    public ApiResponse<FeedResponse> createFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedCreateRequest request) {
        return ApiResponse.ok(feedService.createFeed(userDetails.getUserId(), request));
    }

    @Operation(
            summary = "피드 수정",
            description = """
                    피드의 캡션과 공개 여부를 수정합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    피드 소유자 본인만 수정할 수 있습니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 403 (AUTH_002): 접근 권한 없음 (본인 피드가 아님)
                    - 404 (FEED_001): 피드를 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @PutMapping("/api/v1/feeds/{feedId}")
    public ApiResponse<FeedResponse> updateFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "피드 ID", example = "1") @PathVariable Long feedId,
            @Valid @RequestBody FeedUpdateRequest request) {
        return ApiResponse.ok(feedService.updateFeed(userDetails.getUserId(), feedId, request));
    }

    @Operation(
            summary = "피드 삭제",
            description = """
                    피드를 소프트 삭제합니다 (deleted_at 설정, 이후 조회에서 자동 제외).

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    피드 소유자 본인만 삭제할 수 있습니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 403 (AUTH_002): 접근 권한 없음 (본인 피드가 아님)
                    - 404 (FEED_001): 피드를 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @DeleteMapping("/api/v1/feeds/{feedId}")
    public ApiResponse<Void> deleteFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "피드 ID", example = "1") @PathVariable Long feedId) {
        feedService.deleteFeed(userDetails.getUserId(), feedId);
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "피드 상세 조회",
            description = """
                    피드의 상세 정보를 조회합니다.

                    **인증 선택:** 비인증 시에도 조회 가능하나, myReactionTypes는 빈 배열로 반환됩니다.
                    인증 시 본인의 반응 목록이 포함됩니다.

                    **에러:**
                    - 404 (FEED_001): 피드를 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @GetMapping("/api/v1/feeds/{feedId}")
    public ApiResponse<FeedResponse> getFeedDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "피드 ID", example = "1") @PathVariable Long feedId) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.ok(feedService.getFeedDetail(feedId, currentUserId));
    }

    @Operation(
            summary = "피드 타임라인",
            description = """
                    공개 피드 목록을 최신순으로 조회합니다.

                    **인증 선택:** 비인증 시에도 조회 가능하나, myReactionTypes는 빈 배열로 반환됩니다.

                    **페이지네이션:** 커서 기반 무한 스크롤. cursor 파라미터로 다음 페이지를 요청합니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/api/v1/feeds")
    public ApiResponse<PageResponse<FeedResponse>> getFeedTimeline(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute CursorPageRequest pageRequest) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.ok(feedService.getFeedTimeline(currentUserId, pageRequest));
    }

    @Operation(
            summary = "사용자 피드 목록",
            description = """
                    특정 사용자의 피드 목록을 조회합니다.

                    **인증 선택:** 본인 피드 조회 시 비공개 피드도 포함됩니다.
                    타인 피드 조회 시 공개 피드만 반환됩니다.
                    비인증 시 공개 피드만 반환되며, myReactionTypes는 빈 배열로 반환됩니다.

                    **페이지네이션:** 커서 기반 무한 스크롤.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/api/v1/users/{userId}/feeds")
    public ApiResponse<PageResponse<FeedResponse>> getUserFeeds(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "사용자 ID", example = "1") @PathVariable Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.ok(feedService.getUserFeeds(userId, currentUserId, pageRequest));
    }

    // ── 피드 반응 (토글) ──

    @Operation(
            summary = "피드 반응 토글",
            description = """
                    피드에 반응을 토글합니다.
                    한 유저는 한 피드에 하나의 반응만 가능합니다.

                    - 반응이 없으면 추가
                    - 같은 반응 타입이면 삭제 (토글 off)
                    - 다른 반응 타입이면 교체

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    반응 추가/교체 시 FeedReactionEvent가 발행되어 피드 소유자에게 알림이 전송됩니다.
                    본인 피드에 반응 시 알림은 발송되지 않습니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 404 (FEED_001): 피드를 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "반응 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @PostMapping("/api/v1/feeds/{feedId}/reactions")
    public ApiResponse<ReactionSummary> toggleReaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "피드 ID", example = "1") @PathVariable Long feedId,
            @Parameter(description = "반응 유형", example = "LIKE") @RequestParam ReactionType reactionType) {
        return ApiResponse.ok(feedService.toggleReaction(userDetails.getUserId(), feedId, reactionType));
    }

    @Operation(
            summary = "피드 반응 사용자 목록",
            description = """
                    피드에 반응한 사용자 목록을 조회합니다.
                    각 사용자의 프로필 이미지, 닉네임, 반응 유형을 포함합니다.

                    **인증 선택:** 비인증 시에도 조회 가능합니다.

                    **에러:**
                    - 404 (FEED_001): 피드를 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @GetMapping("/api/v1/feeds/{feedId}/reactions/users")
    public ApiResponse<java.util.List<ReactionUserResponse>> getReactionUsers(
            @Parameter(description = "피드 ID", example = "1") @PathVariable Long feedId) {
        return ApiResponse.ok(feedService.getReactionUsers(feedId));
    }

    // ── 댓글 ──

    @Operation(
            summary = "댓글 작성",
            description = """
                    피드에 댓글 또는 대댓글을 작성합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    대댓글은 자동 플래트닝됩니다 (대댓글의 대댓글 → 원본 댓글의 대댓글로 변환).
                    댓글 작성 시 CommentEvent가 발행되어 피드 소유자 및 부모 댓글 작성자에게 알림이 전송됩니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 404 (FEED_001): 피드를 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @PostMapping("/api/v1/feeds/{feedId}/comments")
    public ApiResponse<CommentResponse> createComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "피드 ID", example = "1") @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.ok(feedService.createComment(userDetails.getUserId(), feedId, request));
    }

    @Operation(
            summary = "댓글 목록 조회",
            description = """
                    피드의 댓글 목록을 조회합니다 (최상위 댓글 + 대댓글 nested 구조).

                    **인증 선택:** 비인증 시에도 조회 가능하나, isLikedByMe는 false로 반환됩니다.

                    **페이지네이션:** 커서 기반 무한 스크롤 (최상위 댓글 기준 페이지네이션).

                    **에러:**
                    - 404 (FEED_001): 피드를 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "피드를 찾을 수 없음")
    })
    @GetMapping("/api/v1/feeds/{feedId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> getComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "피드 ID", example = "1") @PathVariable Long feedId,
            @ModelAttribute CursorPageRequest pageRequest) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.ok(feedService.getComments(feedId, currentUserId, pageRequest));
    }

    @Operation(
            summary = "댓글 수정",
            description = """
                    댓글의 내용(content)을 수정합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    작성자 본인만 수정할 수 있습니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 403 (AUTH_002): 접근 권한 없음 (본인 댓글이 아님)
                    - 404 (FEED_003): 댓글을 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PutMapping("/api/v1/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "댓글 ID", example = "1") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ApiResponse.ok(feedService.updateComment(userDetails.getUserId(), commentId, request));
    }

    @Operation(
            summary = "댓글 삭제",
            description = """
                    댓글을 소프트 삭제합니다 (deleted_at 설정, 이후 조회에서 자동 제외).

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **삭제 권한:** 댓글 작성자 또는 피드 소유자가 삭제할 수 있습니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 403 (AUTH_002): 접근 권한 없음
                    - 404 (FEED_003): 댓글을 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "댓글 ID", example = "1") @PathVariable Long commentId) {
        feedService.deleteComment(userDetails.getUserId(), commentId);
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "댓글 좋아요 토글",
            description = """
                    댓글에 좋아요를 토글합니다 (이미 있으면 삭제, 없으면 추가).

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 404 (FEED_003): 댓글을 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PostMapping("/api/v1/comments/{commentId}/reactions")
    public ApiResponse<Void> toggleCommentLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "댓글 ID", example = "1") @PathVariable Long commentId) {
        feedService.toggleCommentLike(userDetails.getUserId(), commentId);
        return ApiResponse.ok(null);
    }
}
