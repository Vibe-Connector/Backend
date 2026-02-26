package com.link.vibe.domain.auth.controller;

import com.link.vibe.domain.auth.dto.SignupRequest;
import com.link.vibe.domain.auth.dto.TokenResponse;
import com.link.vibe.domain.vibe.service.auth.AuthService;
import com.link.vibe.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
