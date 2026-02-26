package com.link.vibe.domain.vibe.service.user;

import com.link.vibe.domain.user.dto.ProfileImageResponse;
import com.link.vibe.domain.user.dto.UpdateProfileRequest;
import com.link.vibe.domain.user.dto.UserProfileResponse;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import com.link.vibe.global.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponse.from(user);
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
}
