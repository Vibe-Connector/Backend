package com.link.vibe.domain.user.controller;

import com.link.vibe.domain.auth.dto.SocialLoginRequest;
import com.link.vibe.domain.user.dto.ChangePasswordRequest;
import com.link.vibe.domain.user.dto.ProfileImageResponse;
import com.link.vibe.domain.user.dto.SocialAccountResponse;
import com.link.vibe.domain.user.dto.PublicUserProfileResponse;
import com.link.vibe.domain.user.dto.UpdateProfileRequest;
import com.link.vibe.domain.user.dto.UpdateSettingsRequest;
import com.link.vibe.domain.user.dto.UserProfileResponse;
import com.link.vibe.domain.user.dto.UserSettingsResponse;
import com.link.vibe.domain.vibe.service.user.UserService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
            summary = "다른 사용자 프로필 조회",
            description = """
                    userId로 다른 사용자의 공개 프로필을 조회합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    닉네임, 프로필 이미지 등 공개 정보만 반환됩니다.

                    **에러:**
                    - 404 (USER_001): 사용자를 찾을 수 없음
                    """
    )
    @GetMapping("/{userId}")
    public ApiResponse<PublicUserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ApiResponse.ok(userService.getUserProfile(userId));
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

    @Operation(
            summary = "비밀번호 변경",
            description = """
                    현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **에러:**
                    - 400 (USER_004): 현재 비밀번호가 올바르지 않음
                    """
    )
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.changePassword(userId, request);
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    현재 로그인된 사용자의 계정을 비활성화(소프트 삭제)합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    계정 상태가 INACTIVE로 변경되며, 이후 로그인이 차단됩니다.
                    """
    )
    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount() {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.deleteAccount(userId);
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "연동된 소셜 계정 목록 조회",
            description = """
                    현재 사용자에게 연동된 소셜 계정 목록을 조회합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.
                    """
    )
    @GetMapping("/me/social")
    public ApiResponse<List<SocialAccountResponse>> getLinkedSocialAccounts() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.getLinkedSocialAccounts(userId));
    }

    @Operation(
            summary = "소셜 계정 연동",
            description = """
                    현재 사용자에게 소셜 계정을 연동합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    OAuth 인가 코드를 통해 소셜 제공자의 사용자 정보를 조회하고 연동합니다.

                    **에러:**
                    - 409 (USER_005): 이미 연동된 소셜 계정
                    - 400 (AUTH_006): 지원하지 않는 소셜 제공자
                    """
    )
    @PostMapping("/me/social/{provider}")
    public ApiResponse<SocialAccountResponse> linkSocialAccount(
            @Parameter(description = "소셜 제공자 (google, naver)", example = "google")
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.linkSocialAccount(userId, provider, request));
    }

    @Operation(
            summary = "소셜 계정 연동 해제",
            description = """
                    현재 사용자의 소셜 계정 연동을 해제합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    비밀번호가 없고 소셜 계정이 1개뿐인 경우 해제할 수 없습니다.

                    **에러:**
                    - 404 (USER_006): 연동된 소셜 계정을 찾을 수 없음
                    - 400 (USER_007): 마지막 로그인 수단은 해제 불가
                    """
    )
    @DeleteMapping("/me/social/{provider}")
    public ApiResponse<Void> unlinkSocialAccount(
            @Parameter(description = "소셜 제공자 (google, naver)", example = "google")
            @PathVariable String provider) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.unlinkSocialAccount(userId, provider);
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "프로필 이미지 업로드",
            description = """
                    프로필 이미지를 S3에 업로드하고 사용자 프로필에 반영합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **제한:**
                    - 허용 형식: JPEG, PNG, GIF, WebP
                    - 최대 크기: 5MB
                    - 기존 이미지가 있으면 S3에서 삭제 후 교체됩니다.

                    **에러:**
                    - 400 (FILE_002): 파일 크기 초과
                    - 400 (FILE_003): 지원하지 않는 파일 형식
                    """
    )
    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileImageResponse> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.uploadProfileImage(userId, file));
    }

    @Operation(
            summary = "내 설정 조회",
            description = """
                    현재 로그인된 사용자의 설정을 조회합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    설정이 없는 경우 기본값(push=true, email=true, privacy=PRIVATE)으로 자동 생성됩니다.
                    """
    )
    @GetMapping("/me/settings")
    public ApiResponse<UserSettingsResponse> getMySettings() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.getMySettings(userId));
    }

    @Operation(
            summary = "내 설정 수정",
            description = """
                    현재 로그인된 사용자의 설정을 수정합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    전달된 필드만 수정되며, null인 필드는 변경되지 않습니다.
                    """
    )
    @PutMapping("/me/settings")
    public ApiResponse<UserSettingsResponse> updateMySettings(@Valid @RequestBody UpdateSettingsRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.updateMySettings(userId, request));
    }
}
