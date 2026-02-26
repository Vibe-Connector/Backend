package com.link.vibe.domain.auth.controller;

import com.link.vibe.domain.auth.dto.LoginRequest;
import com.link.vibe.domain.auth.dto.RefreshRequest;
import com.link.vibe.domain.auth.dto.SignupRequest;
import com.link.vibe.domain.auth.dto.SocialLoginRequest;
import com.link.vibe.domain.auth.dto.SocialLoginResponse;
import com.link.vibe.domain.auth.dto.TokenResponse;
import com.link.vibe.domain.vibe.service.auth.AuthService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API — 회원가입, 로그인, 토큰 갱신, 로그아웃")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "이메일 회원가입",
            description = """
                    이메일, 비밀번호, 닉네임으로 회원가입합니다.

                    **성공 시:** 사용자 정보와 JWT 토큰(Access + Refresh)을 반환합니다.
                    Refresh Token은 Redis에 저장되며 7일간 유효합니다.

                    **에러:**
                    - 409: 이미 사용 중인 이메일 또는 닉네임
                    - 400: 유효성 검사 실패 (이메일 형식, 비밀번호 8자 미만 등)
                    """
    )
    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @Operation(
            summary = "이메일 로그인",
            description = """
                    이메일과 비밀번호로 로그인합니다.

                    **성공 시:** 사용자 정보와 JWT 토큰(Access + Refresh)을 반환합니다.
                    기존 Refresh Token은 새로 발급된 토큰으로 교체됩니다.

                    **에러:**
                    - 401 (AUTH_003): 이메일 또는 비밀번호 불일치
                    - 403 (AUTH_004): 비활성화된 계정
                    """
    )
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(
            summary = "토큰 갱신",
            description = """
                    Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다.

                    **Refresh Token Rotation:** 갱신 시 Refresh Token도 함께 교체됩니다.
                    기존 Refresh Token은 즉시 무효화되므로, 반드시 새로 받은 토큰을 저장하세요.

                    **에러:**
                    - 401 (AUTH_007): 유효하지 않은 Refresh Token (만료, 위조, Redis 불일치)
                    - 403 (AUTH_004): 비활성화된 계정
                    """
    )
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    현재 로그인된 사용자를 로그아웃합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **동작:** Redis에서 해당 사용자의 Refresh Token을 삭제합니다.
                    Access Token은 만료 시까지 유효하므로, 클라이언트에서도 토큰을 삭제하세요.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청 (토큰 없음 또는 만료)
                    """
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        Long userId = SecurityUtil.getCurrentUserId();
        authService.logout(userId);
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "소셜 로그인",
            description = """
                    소셜 제공자(GOOGLE, NAVER)의 인가 코드로 로그인합니다.

                    **흐름:**
                    1. 프론트엔드에서 소셜 로그인 후 인가 코드(authorizationCode)를 받음
                    2. 인가 코드와 redirectUri를 백엔드에 전달
                    3. 백엔드가 소셜 제공자와 토큰 교환 → 사용자 정보 조회
                    4. 신규 사용자면 자동 가입, 기존 사용자면 로그인 처리

                    **isNewUser=true:** 프론트에서 닉네임 등 추가 정보 입력을 유도하세요.

                    **지원 제공자:** GOOGLE, NAVER

                    **에러:**
                    - 401 (AUTH_005): 소셜 인증 실패 (잘못된 인가 코드 등)
                    - 400 (AUTH_006): 지원하지 않는 제공자
                    - 403 (AUTH_004): 비활성화된 계정
                    """
    )
    @PostMapping("/social/{provider}")
    public ApiResponse<SocialLoginResponse> socialLogin(
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request) {
        return ApiResponse.ok(authService.socialLogin(provider, request));
    }
}
