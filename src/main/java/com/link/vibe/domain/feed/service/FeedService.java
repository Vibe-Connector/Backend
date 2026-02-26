package com.link.vibe.domain.feed.service;

import com.link.vibe.domain.feed.dto.*;
import com.link.vibe.domain.feed.entity.*;
import com.link.vibe.domain.feed.repository.*;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.event.CommentEvent;
import com.link.vibe.global.event.FeedReactionEvent;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedReactionRepository feedReactionRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;
    private final VibeResultRepository vibeResultRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── 피드 CRUD ──

    @Transactional
    public FeedResponse createFeed(Long userId, FeedCreateRequest request) {
        User user = findUser(userId);
        VibeResult vibeResult = vibeResultRepository.findById(request.resultId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VIBE_RESULT_NOT_FOUND));

        if (feedRepository.existsByUserUserIdAndVibeResultResultId(userId, request.resultId())) {
            throw new BusinessException(ErrorCode.FEED_ALREADY_EXISTS);
        }

        Feed feed = Feed.create(user, vibeResult, request.caption(), request.isPublic());
        feedRepository.save(feed);

        return toFeedResponse(feed, userId);
    }

    @Transactional
    public FeedResponse updateFeed(Long userId, Long feedId, FeedUpdateRequest request) {
        Feed feed = findFeed(feedId);
        if (!feed.isOwner(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        feed.update(request.caption(), request.isPublic());
        return toFeedResponse(feed, userId);
    }

    @Transactional
    public void deleteFeed(Long userId, Long feedId) {
        Feed feed = findFeed(feedId);
        if (!feed.isOwner(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        feed.softDelete();
    }

    public FeedResponse getFeedDetail(Long feedId, Long currentUserId) {
        Feed feed = findFeed(feedId);
        return toFeedResponse(feed, currentUserId);
    }

    public PageResponse<FeedResponse> getFeedTimeline(Long currentUserId, CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.hasCursor() ? Long.parseLong(pageRequest.getCursor()) : null;
        List<Feed> feeds = feedRepository.findPublicFeeds(
                cursorId, PageRequest.of(0, pageRequest.getFetchSize()));

        List<FeedResponse> responses = feeds.stream()
                .map(f -> toFeedResponse(f, currentUserId))
                .toList();

        return PageResponse.of(responses, pageRequest.getEffectiveSize(),
                r -> String.valueOf(r.feedId()));
    }

    public PageResponse<FeedResponse> getUserFeeds(Long userId, Long currentUserId,
                                                    CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.hasCursor() ? Long.parseLong(pageRequest.getCursor()) : null;
        List<Feed> feeds = feedRepository.findByUserId(
                userId, cursorId, PageRequest.of(0, pageRequest.getFetchSize()));

        boolean isOwner = userId.equals(currentUserId);
        List<FeedResponse> responses = feeds.stream()
                .filter(f -> isOwner || Boolean.TRUE.equals(f.getIsPublic()))
                .map(f -> toFeedResponse(f, currentUserId))
                .toList();

        return PageResponse.of(responses, pageRequest.getEffectiveSize(),
                r -> String.valueOf(r.feedId()));
    }

    // ── 피드 반응 (토글) ──

    @Transactional
    public ReactionSummary toggleReaction(Long userId, Long feedId, ReactionType reactionType) {
        Feed feed = findFeed(feedId);
        User user = findUser(userId);

        var existing = feedReactionRepository
                .findByFeedFeedIdAndUserUserIdAndReactionType(feedId, userId, reactionType);

        if (existing.isPresent()) {
            feedReactionRepository.delete(existing.get());
        } else {
            FeedReaction reaction = FeedReaction.create(feed, user, reactionType);
            feedReactionRepository.save(reaction);
            eventPublisher.publishEvent(
                    new FeedReactionEvent(feedId, userId, reactionType.getValue()));
        }

        // 해당 반응 유형의 최신 카운트 반환
        long count = feedReactionRepository
                .countByFeedIdGroupByReactionType(feedId).stream()
                .filter(row -> reactionType.equals(row[0]))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);

        return new ReactionSummary(reactionType.getValue(), count);
    }

    // ── 댓글 ──

    @Transactional
    public CommentResponse createComment(Long userId, Long feedId, CommentCreateRequest request) {
        Feed feed = findFeed(feedId);
        User user = findUser(userId);

        FeedComment parentComment = null;
        if (request.parentCommentId() != null) {
            parentComment = findComment(request.parentCommentId());

            // 자동 플래트닝: 대댓글에 답글 → 루트 부모로 변환
            if (parentComment.getParentComment() != null) {
                parentComment = parentComment.getParentComment();
            }
        }

        FeedComment comment = FeedComment.create(feed, user, parentComment, request.content());
        feedCommentRepository.save(comment);

        Long parentId = parentComment != null ? parentComment.getCommentId() : null;
        eventPublisher.publishEvent(
                new CommentEvent(feedId, comment.getCommentId(), userId, parentId));

        return toCommentResponse(comment, userId);
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentUpdateRequest request) {
        FeedComment comment = findComment(commentId);
        if (!comment.isOwner(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        comment.updateContent(request.content());
        return toCommentResponse(comment, userId);
    }

    public PageResponse<CommentResponse> getComments(Long feedId, Long currentUserId,
                                                      CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.hasCursor() ? Long.parseLong(pageRequest.getCursor()) : null;
        List<FeedComment> topLevelComments = feedCommentRepository.findTopLevelComments(
                feedId, cursorId, PageRequest.of(0, pageRequest.getFetchSize()));

        List<CommentResponse> responses = topLevelComments.stream()
                .map(c -> toCommentResponseWithReplies(c, currentUserId))
                .toList();

        return PageResponse.of(responses, pageRequest.getEffectiveSize(),
                r -> String.valueOf(r.commentId()));
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        FeedComment comment = findComment(commentId);

        // 댓글 작성자 또는 피드 작성자만 삭제 가능
        if (!comment.isOwner(userId) && !comment.getFeed().isOwner(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        comment.softDelete();
    }

    // ── 댓글 좋아요 (토글) ──

    @Transactional
    public void toggleCommentLike(Long userId, Long commentId) {
        FeedComment comment = findComment(commentId);
        User user = findUser(userId);

        var existing = commentReactionRepository
                .findByCommentCommentIdAndUserUserId(commentId, userId);

        if (existing.isPresent()) {
            commentReactionRepository.delete(existing.get());
        } else {
            CommentReaction reaction = CommentReaction.create(comment, user);
            commentReactionRepository.save(reaction);
        }
    }

    // ── private 헬퍼 ──

    private Feed findFeed(Long feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private FeedComment findComment(Long commentId) {
        return feedCommentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private FeedResponse toFeedResponse(Feed feed, Long currentUserId) {
        List<ReactionSummary> reactions = getReactionSummary(feed.getFeedId());
        long commentCount = feedCommentRepository.countByFeedFeedId(feed.getFeedId());
        List<String> myReactionTypes = getMyReactionTypes(feed.getFeedId(), currentUserId);

        VibeResult vr = feed.getVibeResult();

        return new FeedResponse(
                feed.getFeedId(),
                feed.getUser().getUserId(),
                feed.getUser().getNickname(),
                feed.getUser().getProfileImageUrl(),
                vr.getResultId(),
                vr.getGeneratedImageUrl(),
                vr.getPhrase(),
                feed.getCaption(),
                feed.getIsPublic(),
                feed.getViewCount(),
                reactions,
                commentCount,
                myReactionTypes,
                feed.getCreatedAt(),
                feed.getUpdatedAt()
        );
    }

    private CommentResponse toCommentResponse(FeedComment comment, Long currentUserId) {
        long likeCount = commentReactionRepository.countByCommentCommentId(comment.getCommentId());
        boolean isLikedByMe = currentUserId != null &&
                commentReactionRepository.existsByCommentCommentIdAndUserUserId(
                        comment.getCommentId(), currentUserId);

        Long parentCommentId = comment.getParentComment() != null
                ? comment.getParentComment().getCommentId() : null;

        return new CommentResponse(
                comment.getCommentId(),
                comment.getFeed().getFeedId(),
                comment.getUser().getUserId(),
                comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(),
                parentCommentId,
                comment.getContent(),
                likeCount,
                isLikedByMe,
                Collections.emptyList(),
                comment.getCreatedAt()
        );
    }

    private CommentResponse toCommentResponseWithReplies(FeedComment comment, Long currentUserId) {
        long likeCount = commentReactionRepository.countByCommentCommentId(comment.getCommentId());
        boolean isLikedByMe = currentUserId != null &&
                commentReactionRepository.existsByCommentCommentIdAndUserUserId(
                        comment.getCommentId(), currentUserId);

        List<CommentResponse> replies = feedCommentRepository
                .findByParentCommentCommentIdOrderByCommentIdAsc(comment.getCommentId())
                .stream()
                .map(reply -> toCommentResponse(reply, currentUserId))
                .toList();

        return new CommentResponse(
                comment.getCommentId(),
                comment.getFeed().getFeedId(),
                comment.getUser().getUserId(),
                comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(),
                null,
                comment.getContent(),
                likeCount,
                isLikedByMe,
                replies,
                comment.getCreatedAt()
        );
    }

    private List<ReactionSummary> getReactionSummary(Long feedId) {
        return feedReactionRepository.countByFeedIdGroupByReactionType(feedId).stream()
                .map(row -> new ReactionSummary(
                        ((ReactionType) row[0]).getValue(),
                        (Long) row[1]))
                .toList();
    }

    private List<String> getMyReactionTypes(Long feedId, Long userId) {
        if (userId == null) return Collections.emptyList();
        return feedReactionRepository.findByFeedFeedIdAndUserUserId(feedId, userId).stream()
                .map(r -> r.getReactionType().getValue())
                .toList();
    }
}
