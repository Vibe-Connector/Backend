package com.link.vibe.domain.follow.service;

import com.link.vibe.domain.follow.dto.FollowResponse;
import com.link.vibe.domain.follow.dto.FollowUserResponse;
import com.link.vibe.domain.follow.entity.Follow;
import com.link.vibe.domain.follow.repository.FollowRepository;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.event.FollowEvent;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FollowResponse follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (followRepository.existsByFollowerUserIdAndFollowingUserId(followerId, followingId)) {
            throw new BusinessException(ErrorCode.FOLLOW_ALREADY_EXISTS);
        }

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();
        followRepository.save(follow);

        eventPublisher.publishEvent(new FollowEvent(followerId, followingId));

        long followerCount = followRepository.countByFollowingUserId(followingId);
        return new FollowResponse(true, followerCount);
    }

    @Transactional
    public FollowResponse unfollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }

        Follow follow = followRepository.findByFollowerUserIdAndFollowingUserId(followerId, followingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLLOW_NOT_FOUND));
        followRepository.delete(follow);

        long followerCount = followRepository.countByFollowingUserId(followingId);
        return new FollowResponse(false, followerCount);
    }

    public FollowResponse getFollowStatus(Long currentUserId, Long targetUserId) {
        boolean isFollowing = followRepository.existsByFollowerUserIdAndFollowingUserId(currentUserId, targetUserId);
        long followerCount = followRepository.countByFollowingUserId(targetUserId);
        return new FollowResponse(isFollowing, followerCount);
    }

    public PageResponse<FollowUserResponse> getFollowers(Long targetUserId, Long currentUserId, CursorPageRequest pageRequest) {
        int fetchSize = pageRequest.getFetchSize();
        PageRequest pageable = PageRequest.of(0, fetchSize);

        List<Follow> follows;
        if (pageRequest.hasCursor()) {
            Long cursor = Long.parseLong(pageRequest.getCursor());
            follows = followRepository.findFollowersByCursor(targetUserId, cursor, pageable);
        } else {
            follows = followRepository.findFollowers(targetUserId, pageable);
        }

        Set<Long> myFollowingIds = getMyFollowingIds(currentUserId, follows.stream()
                .map(f -> f.getFollower().getUserId())
                .collect(Collectors.toList()));

        List<FollowUserResponse> content = follows.stream()
                .map(f -> FollowUserResponse.of(f.getFollower(), myFollowingIds.contains(f.getFollower().getUserId())))
                .collect(Collectors.toList());

        return PageResponse.of(content, pageRequest.getEffectiveSize(),
                item -> String.valueOf(follows.get(content.indexOf(item)).getFollowId()));
    }

    public PageResponse<FollowUserResponse> getFollowings(Long targetUserId, Long currentUserId, CursorPageRequest pageRequest) {
        int fetchSize = pageRequest.getFetchSize();
        PageRequest pageable = PageRequest.of(0, fetchSize);

        List<Follow> follows;
        if (pageRequest.hasCursor()) {
            Long cursor = Long.parseLong(pageRequest.getCursor());
            follows = followRepository.findFollowingsByCursor(targetUserId, cursor, pageable);
        } else {
            follows = followRepository.findFollowings(targetUserId, pageable);
        }

        Set<Long> myFollowingIds = getMyFollowingIds(currentUserId, follows.stream()
                .map(f -> f.getFollowing().getUserId())
                .collect(Collectors.toList()));

        List<FollowUserResponse> content = follows.stream()
                .map(f -> FollowUserResponse.of(f.getFollowing(), myFollowingIds.contains(f.getFollowing().getUserId())))
                .collect(Collectors.toList());

        return PageResponse.of(content, pageRequest.getEffectiveSize(),
                item -> String.valueOf(follows.get(content.indexOf(item)).getFollowId()));
    }

    private Set<Long> getMyFollowingIds(Long currentUserId, List<Long> userIds) {
        return userIds.stream()
                .filter(id -> followRepository.existsByFollowerUserIdAndFollowingUserId(currentUserId, id))
                .collect(Collectors.toSet());
    }
}
