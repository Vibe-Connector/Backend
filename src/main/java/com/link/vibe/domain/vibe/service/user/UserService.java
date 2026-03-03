package com.link.vibe.domain.vibe.service.user;

import com.link.vibe.domain.auth.dto.SocialLoginRequest;
import com.link.vibe.domain.auth.oauth.OAuthClientFactory;
import com.link.vibe.domain.auth.oauth.OAuthUserInfo;
import com.link.vibe.domain.user.dto.ChangePasswordRequest;
import com.link.vibe.domain.user.dto.ProfileImageResponse;
import com.link.vibe.domain.user.dto.PublicUserProfileResponse;
import com.link.vibe.domain.user.dto.SocialAccountResponse;
import com.link.vibe.domain.user.dto.UpdateProfileRequest;
import com.link.vibe.domain.user.dto.UpdateSettingsRequest;
import com.link.vibe.domain.user.dto.UserProfileResponse;
import com.link.vibe.domain.user.dto.UserSettingsResponse;
import com.link.vibe.domain.user.entity.SocialAccount;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.entity.UserSettings;
import com.link.vibe.domain.user.repository.SocialAccountRepository;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.user.repository.UserSettingsRepository;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import com.link.vibe.global.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final S3StorageService s3StorageService;
    private final PasswordEncoder passwordEncoder;
    private final OAuthClientFactory oAuthClientFactory;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public PublicUserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return PublicUserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 닉네임 변경 시 중복 확인
        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.nickname())) {
                throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        user.updateProfile(
                request.nickname(),
                request.name(),
                request.gender(),
                request.birthYear(),
                request.profileImageUrl(),
                request.preferredLanguageId()
        );

        return UserProfileResponse.from(user);
    }

    @Transactional
    public ProfileImageResponse uploadProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 기존 이미지가 S3에 있으면 삭제
        if (user.getProfileImageUrl() != null) {
            s3StorageService.delete(user.getProfileImageUrl());
        }

        String imageUrl = s3StorageService.upload("profiles", file);
        user.updateProfileImageUrl(imageUrl);

        return new ProfileImageResponse(imageUrl);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public SocialAccountResponse linkSocialAccount(Long userId, String provider, SocialLoginRequest request) {
        String upperProvider = provider.toUpperCase();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미 연동된 소셜 계정 확인
        if (socialAccountRepository.findByUserUserIdAndProvider(userId, upperProvider).isPresent()) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }

        // OAuth 인가 코드로 사용자 정보 조회
        OAuthUserInfo userInfo = oAuthClientFactory.getClient(upperProvider)
                .getUserInfo(request.authorizationCode(), request.redirectUri());

        // 다른 유저에 이미 연동된 계정인지 확인
        if (socialAccountRepository.findByProviderAndProviderUserId(upperProvider, userInfo.getProviderUserId()).isPresent()) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }

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

        return SocialAccountResponse.from(socialAccount);
    }

    @Transactional
    public void unlinkSocialAccount(Long userId, String provider) {
        String upperProvider = provider.toUpperCase();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        SocialAccount socialAccount = socialAccountRepository.findByUserUserIdAndProvider(userId, upperProvider)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOCIAL_ACCOUNT_NOT_FOUND));

        // 마지막 로그인 수단 보호: 비밀번호도 없고 소셜 계정이 1개뿐이면 해제 불가
        boolean hasPassword = user.getPassword() != null;
        long socialCount = socialAccountRepository.findAllByUserUserId(userId).size();
        if (!hasPassword && socialCount <= 1) {
            throw new BusinessException(ErrorCode.CANNOT_UNLINK_LAST_AUTH);
        }

        socialAccountRepository.delete(socialAccount);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.deactivate();
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getMySettings(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return UserSettingsResponse.from(settings);
    }

    @Transactional
    public UserSettingsResponse updateMySettings(Long userId, UpdateSettingsRequest request) {
        UserSettings settings = userSettingsRepository.findByUserUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.update(
                request.pushEnabled(),
                request.emailNotification(),
                request.defaultSharePrivacy()
        );

        return UserSettingsResponse.from(settings);
    }

    private UserSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userSettingsRepository.save(UserSettings.builder().user(user).build());
    }
}
