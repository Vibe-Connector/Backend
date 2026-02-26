package com.link.vibe.domain.user.controller;

import com.link.vibe.domain.user.dto.UpdateProfileRequest;
import com.link.vibe.domain.user.dto.UserProfileResponse;
import com.link.vibe.domain.vibe.service.user.UserService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API — 프로필 조회/수정")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 프로필 조회",
            description = """
                    현재 로그인된 사용자의 프로필 정보를 조회합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.
                    """
    )
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.getMyProfile(userId));
    }

    @Operation(
            summary = "내 프로필 수정",
            description = """
                    현재 로그인된 사용자의 프로필 정보를 수정합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    전달된 필드만 수정되며, null인 필드는 변경되지 않습니다.

                    **에러:**
                    - 409 (USER_003): 이미 사용 중인 닉네임
                    """
    )
    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.updateMyProfile(userId, request));
    }
}
