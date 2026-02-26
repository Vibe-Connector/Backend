package com.link.vibe.domain.feed.controller;

import com.link.vibe.domain.feed.dto.*;
import com.link.vibe.domain.feed.entity.ReactionType;
import com.link.vibe.domain.feed.service.FeedService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feeds", description = "피드 + 댓글 API")
@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    // ── 피드 CRUD ──

    @Operation(summary = "피드 작성", description = "Vibe 결과를 피드로 공유")
    @PostMapping("/api/v1/feeds")
    public ApiResponse<FeedResponse> createFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FeedCreateRequest request) {
        return ApiResponse.ok(feedService.createFeed(userDetails.getUserId(), request));
    }

    @Operation(summary = "피드 수정", description = "캡션, 공개 여부 수정")
    @PutMapping("/api/v1/feeds/{feedId}")
    public ApiResponse<FeedResponse> updateFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedId,
            @Valid @RequestBody FeedUpdateRequest request) {
        return ApiResponse.ok(feedService.updateFeed(userDetails.getUserId(), feedId, request));
    }

    @Operation(summary = "피드 삭제", description = "소프트 삭제")
    @DeleteMapping("/api/v1/feeds/{feedId}")
    public ApiResponse<Void> deleteFeed(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedId) {
        feedService.deleteFeed(userDetails.getUserId(), feedId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "피드 상세 조회")
    @GetMapping("/api/v1/feeds/{feedId}")
    public ApiResponse<FeedResponse> getFeedDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedId) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.ok(feedService.getFeedDetail(feedId, currentUserId));
    }

    @Operation(summary = "피드 타임라인", description = "공개 피드 목록, 커서 기반 페이지네이션")
    @GetMapping("/api/v1/feeds")
    public ApiResponse<PageResponse<FeedResponse>> getFeedTimeline(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute CursorPageRequest pageRequest) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.ok(feedService.getFeedTimeline(currentUserId, pageRequest));
    }

    @Operation(summary = "사용자 피드 목록")
    @GetMapping("/api/v1/users/{userId}/feeds")
    public ApiResponse<PageResponse<FeedResponse>> getUserFeeds(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.ok(feedService.getUserFeeds(userId, currentUserId, pageRequest));
    }

    // ── 피드 반응 (토글) ──

    @Operation(summary = "피드 반응 토글", description = "이미 있으면 삭제, 없으면 추가")
    @PostMapping("/api/v1/feeds/{feedId}/reactions")
    public ApiResponse<ReactionSummary> toggleReaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedId,
            @RequestParam ReactionType reactionType) {
        return ApiResponse.ok(feedService.toggleReaction(userDetails.getUserId(), feedId, reactionType));
    }

    // ── 댓글 ──

    @Operation(summary = "댓글 작성")
    @PostMapping("/api/v1/feeds/{feedId}/comments")
    public ApiResponse<CommentResponse> createComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.ok(feedService.createComment(userDetails.getUserId(), feedId, request));
    }

    @Operation(summary = "댓글 목록 조회", description = "최상위 댓글 + 대댓글 nested")
    @GetMapping("/api/v1/feeds/{feedId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> getComments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long feedId,
            @ModelAttribute CursorPageRequest pageRequest) {
        Long currentUserId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.ok(feedService.getComments(feedId, currentUserId, pageRequest));
    }

    @Operation(summary = "댓글 수정", description = "content만 수정 가능, 작성자 본인만")
    @PutMapping("/api/v1/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ApiResponse.ok(feedService.updateComment(userDetails.getUserId(), commentId, request));
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId) {
        feedService.deleteComment(userDetails.getUserId(), commentId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "댓글 좋아요 토글", description = "이미 있으면 삭제, 없으면 추가")
    @PostMapping("/api/v1/comments/{commentId}/reactions")
    public ApiResponse<Void> toggleCommentLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId) {
        feedService.toggleCommentLike(userDetails.getUserId(), commentId);
        return ApiResponse.ok(null);
    }
}
