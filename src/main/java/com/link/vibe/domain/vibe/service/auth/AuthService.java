package com.link.vibe.domain.vibe.service.auth;

import com.link.vibe.domain.auth.dto.LoginRequest;
import com.link.vibe.domain.auth.dto.RefreshRequest;
import com.link.vibe.domain.auth.dto.SignupRequest;
import com.link.vibe.domain.auth.dto.SocialLoginRequest;
import com.link.vibe.domain.auth.dto.SocialLoginResponse;
import com.link.vibe.domain.auth.dto.SocialSignupRequest;
import com.link.vibe.domain.auth.dto.TokenResponse;
import com.link.vibe.domain.auth.oauth.OAuthClient;
import com.link.vibe.domain.auth.oauth.OAuthClientFactory;
import com.link.vibe.domain.auth.oauth.OAuthUserInfo;
import com.link.vibe.domain.auth.service.EmailVerificationService;
import com.link.vibe.domain.user.entity.SocialAccount;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.entity.UserSettings;
import com.link.vibe.domain.user.repository.SocialAccountRepository;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.user.repository.UserSettingsRepository;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import com.link.vibe.global.security.JwtTokenProvider;
import com.link.vibe.global.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final OAuthClientFactory oAuthClientFactory;
    private final EmailVerificationService emailVerificationService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SOCIAL_SIGNUP_KEY_PREFIX = "social_signup:";
    private static final long SOCIAL_SIGNUP_TTL_MINUTES = 15;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        // 이메일 인증 완료 여부 확인
        if (!emailVerificationService.isVerified(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

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
        userSettingsRepository.save(UserSettings.builder().user(savedUser).build());

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

        if (existingSocial.isEmpty()) {
            // 3a. 이메일로 기존 유저 조회
            var existingUser = userInfo.getEmail() != null
                    ? userRepository.findByEmail(userInfo.getEmail()) : java.util.Optional.<User>empty();

            if (existingUser.isPresent()) {
                // 기존 유저가 있지만 소셜 연동이 없음 → 자동 재연동 차단
                throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_NOT_LINKED);
            }

            // 신규 유저: DB에 저장하지 않고 Redis에 OAuth 정보 임시 저장
            String socialSignupToken = UUID.randomUUID().toString();
            String redisKey = SOCIAL_SIGNUP_KEY_PREFIX + socialSignupToken;

            Map<String, String> oauthData = new HashMap<>();
            oauthData.put("provider", upperProvider);
            oauthData.put("providerUserId", userInfo.getProviderUserId());
            oauthData.put("email", userInfo.getEmail() != null ? userInfo.getEmail() : "");
            oauthData.put("name", userInfo.getName() != null ? userInfo.getName() : "");
            oauthData.put("profileImageUrl", userInfo.getProfileImageUrl() != null ? userInfo.getProfileImageUrl() : "");
            oauthData.put("oauthAccessToken", userInfo.getAccessToken() != null ? userInfo.getAccessToken() : "");
            oauthData.put("oauthRefreshToken", userInfo.getRefreshToken() != null ? userInfo.getRefreshToken() : "");
            oauthData.put("expiresIn", userInfo.getExpiresIn() != null ? String.valueOf(userInfo.getExpiresIn()) : "");

            redisTemplate.opsForHash().putAll(redisKey, oauthData);
            redisTemplate.expire(redisKey, SOCIAL_SIGNUP_TTL_MINUTES, TimeUnit.MINUTES);

            return new SocialLoginResponse(
                    null,
                    userInfo.getEmail(),
                    null,
                    userInfo.getProfileImageUrl(),
                    null,
                    null,
                    true,
                    socialSignupToken
            );
        }

        // 3b. 기존 유저 — 토큰 갱신
        SocialAccount socialAccount = existingSocial.get();
        LocalDateTime tokenExpiresAt = userInfo.getExpiresIn() != null
                ? LocalDateTime.now().plusSeconds(userInfo.getExpiresIn()) : null;
        socialAccount.updateTokens(userInfo.getAccessToken(), userInfo.getRefreshToken(), tokenExpiresAt);
        User user = socialAccount.getUser();

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
                false,
                null
        );
    }

    @Transactional(readOnly = true)
    public boolean checkEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * 이메일 사용 가능 여부를 확인하고, 이미 사용 중이면 예외를 던집니다.
     */
    @Transactional(readOnly = true)
    public void checkEmailAvailableOrThrow(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Transactional(readOnly = true)
    public boolean checkNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    @Transactional
    public TokenResponse socialSignup(SocialSignupRequest request) {
        String redisKey = SOCIAL_SIGNUP_KEY_PREFIX + request.socialSignupToken();
        Map<Object, Object> oauthData = redisTemplate.opsForHash().entries(redisKey);

        if (oauthData.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SOCIAL_SIGNUP_TOKEN);
        }

        String provider = (String) oauthData.get("provider");
        String providerUserId = (String) oauthData.get("providerUserId");
        String email = (String) oauthData.get("email");
        String name = (String) oauthData.get("name");
        String profileImageUrl = (String) oauthData.get("profileImageUrl");
        String oauthAccessToken = (String) oauthData.get("oauthAccessToken");
        String oauthRefreshToken = (String) oauthData.get("oauthRefreshToken");
        String expiresInStr = (String) oauthData.get("expiresIn");

        // 이메일 중복 확인
        if (email != null && !email.isBlank() && userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 닉네임 중복 확인
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 유저 생성
        User user = User.builder()
                .email(email != null && !email.isBlank() ? email : null)
                .password(passwordEncoder.encode(request.password()))
                .name(name != null && !name.isBlank() ? name : null)
                .nickname(request.nickname())
                .profileImageUrl(profileImageUrl != null && !profileImageUrl.isBlank() ? profileImageUrl : null)
                .build();
        User savedUser = userRepository.save(user);
        userSettingsRepository.save(UserSettings.builder().user(savedUser).build());

        // 소셜 계정 연동
        Long expiresIn = (expiresInStr != null && !expiresInStr.isBlank()) ? Long.parseLong(expiresInStr) : null;
        LocalDateTime tokenExpiresAt = expiresIn != null
                ? LocalDateTime.now().plusSeconds(expiresIn) : null;

        SocialAccount socialAccount = SocialAccount.builder()
                .user(savedUser)
                .provider(provider)
                .providerUserId(providerUserId)
                .accessToken(oauthAccessToken != null && !oauthAccessToken.isBlank() ? oauthAccessToken : null)
                .refreshToken(oauthRefreshToken != null && !oauthRefreshToken.isBlank() ? oauthRefreshToken : null)
                .tokenExpiresAt(tokenExpiresAt)
                .build();
        socialAccountRepository.save(socialAccount);

        // Redis에서 임시 데이터 삭제
        redisTemplate.delete(redisKey);

        // JWT 발급
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
