package com.link.vibe.domain.follow.controller;

import com.link.vibe.domain.follow.dto.FollowResponse;
import com.link.vibe.domain.follow.dto.FollowUserResponse;
import com.link.vibe.domain.follow.service.FollowService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Follow", description = "팔로우/언팔로우 API")
@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(
            summary = "팔로우",
            description = """
                    대상 사용자를 팔로우합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **에러:**
                    - 400 (FOLLOW_003): 자기 자신을 팔로우할 수 없음
                    - 404 (USER_001): 사용자를 찾을 수 없음
                    - 409 (FOLLOW_001): 이미 팔로우한 사용자
                    """
    )
    @PostMapping("/follow")
    public ApiResponse<FollowResponse> follow(
            @Parameter(description = "팔로우할 대상 사용자 ID", example = "1")
            @PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(followService.follow(currentUserId, userId));
    }

    @Operation(
            summary = "언팔로우",
            description = """
                    대상 사용자를 언팔로우합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **에러:**
                    - 400 (FOLLOW_003): 자기 자신을 언팔로우할 수 없음
                    - 404 (FOLLOW_002): 팔로우 관계를 찾을 수 없음
                    """
    )
    @DeleteMapping("/follow")
    public ApiResponse<FollowResponse> unfollow(
            @Parameter(description = "언팔로우할 대상 사용자 ID", example = "1")
            @PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(followService.unfollow(currentUserId, userId));
    }

    @Operation(
            summary = "팔로우 상태 확인",
            description = """
                    대상 사용자에 대한 팔로우 상태와 팔로워 수를 조회합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.
                    """
    )
    @GetMapping("/follow/status")
    public ApiResponse<FollowResponse> getFollowStatus(
            @Parameter(description = "조회할 대상 사용자 ID", example = "1")
            @PathVariable Long userId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(followService.getFollowStatus(currentUserId, userId));
    }

    @Operation(
            summary = "팔로워 목록",
            description = """
                    대상 사용자의 팔로워 목록을 조회합니다. 커서 기반 페이지네이션을 지원합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    각 사용자에 대해 현재 로그인 사용자의 팔로우 여부(`following`)가 포함됩니다.
                    """
    )
    @GetMapping("/followers")
    public ApiResponse<PageResponse<FollowUserResponse>> getFollowers(
            @Parameter(description = "대상 사용자 ID", example = "1")
            @PathVariable Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(followService.getFollowers(userId, currentUserId, pageRequest));
    }

    @Operation(
            summary = "팔로잉 목록",
            description = """
                    대상 사용자가 팔로우하는 사용자 목록을 조회합니다. 커서 기반 페이지네이션을 지원합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    각 사용자에 대해 현재 로그인 사용자의 팔로우 여부(`following`)가 포함됩니다.
                    """
    )
    @GetMapping("/following")
    public ApiResponse<PageResponse<FollowUserResponse>> getFollowings(
            @Parameter(description = "대상 사용자 ID", example = "1")
            @PathVariable Long userId,
            @ModelAttribute CursorPageRequest pageRequest) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(followService.getFollowings(userId, currentUserId, pageRequest));
    }
}
