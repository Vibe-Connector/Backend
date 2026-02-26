package com.link.vibe.domain.vibe.service.auth;

import com.link.vibe.domain.auth.dto.LoginRequest;
import com.link.vibe.domain.auth.dto.RefreshRequest;
import com.link.vibe.domain.auth.dto.SignupRequest;
import com.link.vibe.domain.auth.dto.SocialLoginRequest;
import com.link.vibe.domain.auth.dto.SocialLoginResponse;
import com.link.vibe.domain.auth.dto.TokenResponse;
import com.link.vibe.domain.auth.oauth.OAuthClient;
import com.link.vibe.domain.auth.oauth.OAuthClientFactory;
import com.link.vibe.domain.auth.oauth.OAuthUserInfo;
import com.link.vibe.domain.user.entity.SocialAccount;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.SocialAccountRepository;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import com.link.vibe.global.security.JwtTokenProvider;
import com.link.vibe.global.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final OAuthClientFactory oAuthClientFactory;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getUserId(), savedUser.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getUserId(), savedUser.getEmail());

        refreshTokenService.save(savedUser.getUserId(), refreshToken);

        return new TokenResponse(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getNickname(),
                savedUser.getProfileImageUrl(),
                accessToken,
                refreshToken
        );
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        user.updateLastLoginAt();

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getEmail());

        refreshTokenService.save(user.getUserId(), refreshToken);

        return new TokenResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                accessToken,
                refreshToken
        );
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();

        // 1. 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. REFRESH 타입인지 확인
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. Redis에 저장된 토큰과 일치하는지 확인
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        if (!refreshTokenService.validate(userId, refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 4. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 5. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getEmail());

        // 6. Redis에 새 Refresh Token 저장 (기존 토큰 교체)
        refreshTokenService.save(user.getUserId(), newRefreshToken);

        return new TokenResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                newAccessToken,
                newRefreshToken
        );
    }

    public void logout(Long userId) {
        refreshTokenService.delete(userId);
    }

    @Transactional
    public SocialLoginResponse socialLogin(String provider, SocialLoginRequest request) {
        String upperProvider = provider.toUpperCase();
        OAuthClient oAuthClient = oAuthClientFactory.getClient(upperProvider);

        // 1. OAuth 제공자로부터 사용자 정보 조회
        OAuthUserInfo userInfo = oAuthClient.getUserInfo(request.authorizationCode(), request.redirectUri());

        // 2. 기존 소셜 계정 조회
        var existingSocial = socialAccountRepository
                .findByProviderAndProviderUserId(upperProvider, userInfo.getProviderUserId());

        boolean isNewUser = existingSocial.isEmpty();
        User user;

        if (isNewUser) {
            // 3a. 신규 — 이메일로 기존 유저 조회 또는 신규 생성
            user = userRepository.findByEmail(userInfo.getEmail())
                    .orElseGet(() -> {
                        String nickname = generateUniqueNickname(userInfo.getName());
                        User newUser = User.builder()
                                .email(userInfo.getEmail())
                                .name(userInfo.getName())
                                .nickname(nickname)
                                .profileImageUrl(userInfo.getProfileImageUrl())
                                .build();
                        return userRepository.save(newUser);
                    });

            // 소셜 계정 연동
            LocalDateTime tokenExpiresAt = userInfo.getExpiresIn() != null
                    ? LocalDateTime.now().plusSeconds(userInfo.getExpiresIn()) : null;

            SocialAccount socialAccount = SocialAccount.builder()
                    .user(user)
                    .provider(upperProvider)
                    .providerUserId(userInfo.getProviderUserId())
                    .accessToken(userInfo.getAccessToken())
                    .refreshToken(userInfo.getRefreshToken())
                    .tokenExpiresAt(tokenExpiresAt)
                    .build();
            socialAccountRepository.save(socialAccount);
        } else {
            // 3b. 기존 — 토큰 갱신
            SocialAccount socialAccount = existingSocial.get();
            LocalDateTime tokenExpiresAt = userInfo.getExpiresIn() != null
                    ? LocalDateTime.now().plusSeconds(userInfo.getExpiresIn()) : null;
            socialAccount.updateTokens(userInfo.getAccessToken(), userInfo.getRefreshToken(), tokenExpiresAt);
            user = socialAccount.getUser();
        }

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        user.updateLastLoginAt();

        // 4. JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getEmail());
        refreshTokenService.save(user.getUserId(), refreshToken);

        return new SocialLoginResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                accessToken,
                refreshToken,
                isNewUser
        );
    }

    @Transactional(readOnly = true)
    public boolean checkEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean checkNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    private String generateUniqueNickname(String baseName) {
        String base = (baseName != null && !baseName.isBlank()) ? baseName : "user";
        String nickname = base;
        int suffix = 1;
        while (userRepository.existsByNickname(nickname)) {
            nickname = base + suffix++;
        }
        return nickname;
    }
}
