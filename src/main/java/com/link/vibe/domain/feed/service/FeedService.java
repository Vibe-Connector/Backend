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

import java.util.*;

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

    @Transactional
    public FeedResponse getFeedDetail(Long feedId, Long currentUserId) {
        Feed feed = findFeed(feedId);

        // 타인 게시물 조회 시에만 조회수 증가 (비인증 또는 본인이 아닌 경우)
        if (currentUserId == null || !currentUserId.equals(feed.getUser().getUserId())) {
            feed.incrementViewCount();
        }

        return toFeedResponse(feed, currentUserId);
    }

    public PageResponse<FeedResponse> getFeedTimeline(Long currentUserId, CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.hasCursor() ? Long.parseLong(pageRequest.getCursor()) : null;
        List<Feed> feeds = feedRepository.findPublicFeedsWithFetch(
                cursorId, PageRequest.of(0, pageRequest.getFetchSize()));

        List<FeedResponse> responses = toFeedResponses(feeds, currentUserId);

        return PageResponse.of(responses, pageRequest.getEffectiveSize(),
                r -> String.valueOf(r.feedId()));
    }

    public PageResponse<FeedResponse> getUserFeeds(Long userId, Long currentUserId,
                                                    CursorPageRequest pageRequest) {
        Long cursorId = pageRequest.hasCursor() ? Long.parseLong(pageRequest.getCursor()) : null;
        List<Feed> feeds = feedRepository.findByUserIdWithFetch(
                userId, cursorId, PageRequest.of(0, pageRequest.getFetchSize()));

        boolean isOwner = userId.equals(currentUserId);
        List<Feed> filtered = feeds.stream()
                .filter(f -> isOwner || Boolean.TRUE.equals(f.getIsPublic()))
                .toList();
        List<FeedResponse> responses = toFeedResponses(filtered, currentUserId);

        return PageResponse.of(responses, pageRequest.getEffectiveSize(),
                r -> String.valueOf(r.feedId()));
    }

    // ── 피드 반응 (토글) — 한 유저 한 피드당 하나의 반응만 가능 ──

    @Transactional
    public ReactionSummary toggleReaction(Long userId, Long feedId, ReactionType reactionType) {
        Feed feed = findFeed(feedId);
        User user = findUser(userId);

        var existing = feedReactionRepository.findByFeedFeedIdAndUserUserId(feedId, userId);

        if (existing.isPresent()) {
            FeedReaction current = existing.get();
            if (current.getReactionType() == reactionType) {
                // 같은 타입 → 삭제 (토글 off)
                feedReactionRepository.delete(current);
            } else {
                // 다른 타입 → 교체
                current.changeReactionType(reactionType);
                eventPublisher.publishEvent(
                        new FeedReactionEvent(feedId, userId, reactionType.getValue()));
            }
        } else {
            // 반응 없음 → 새로 생성
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

    // ── 피드 반응 사용자 목록 ──

    public List<ReactionUserResponse> getReactionUsers(Long feedId) {
        findFeed(feedId); // 피드 존재 확인
        return feedReactionRepository.findAllWithUserByFeedId(feedId).stream()
                .map(fr -> new ReactionUserResponse(
                        fr.getUser().getUserId(),
                        fr.getUser().getNickname(),
                        fr.getUser().getProfileImageUrl(),
                        fr.getReactionType().getValue()))
                .toList();
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

    /**
     * 배치 변환: N+1 쿼리를 3개 배치 쿼리로 최적화
     * 기존: 1 + N×3 쿼리 → 최적화: 1 + 3 쿼리
     */
    private List<FeedResponse> toFeedResponses(List<Feed> feeds, Long currentUserId) {
        if (feeds.isEmpty()) return Collections.emptyList();

        List<Long> feedIds = feeds.stream().map(Feed::getFeedId).toList();

        // 배치 1: 전체 피드의 반응 카운트 (feedId → List<ReactionSummary>)
        Map<Long, List<ReactionSummary>> reactionsMap = new HashMap<>();
        feedReactionRepository.countByFeedIdsGroupByReactionType(feedIds)
                .forEach(row -> {
                    Long feedId = (Long) row[0];
                    String type = ((ReactionType) row[1]).getValue();
                    Long count = (Long) row[2];
                    reactionsMap.computeIfAbsent(feedId, k -> new ArrayList<>())
                            .add(new ReactionSummary(type, count));
                });

        // 배치 2: 전체 피드의 댓글 카운트 (feedId → count)
        Map<Long, Long> commentCountMap = new HashMap<>();
        feedCommentRepository.countByFeedFeedIdIn(feedIds)
                .forEach(row -> commentCountMap.put((Long) row[0], (Long) row[1]));

        // 배치 3: 현재 유저의 반응 타입 (feedId → List<String>)
        Map<Long, List<String>> myReactionsMap = new HashMap<>();
        if (currentUserId != null) {
            feedReactionRepository.findByFeedFeedIdInAndUserUserId(feedIds, currentUserId)
                    .forEach(fr -> myReactionsMap.put(
                            fr.getFeed().getFeedId(),
                            List.of(fr.getReactionType().getValue())));
        }

        return feeds.stream().map(feed -> {
            Long fid = feed.getFeedId();
            VibeResult vr = feed.getVibeResult();
            return new FeedResponse(
                    fid,
                    feed.getUser().getUserId(),
                    feed.getUser().getNickname(),
                    feed.getUser().getProfileImageUrl(),
                    vr.getResultId(),
                    vr.getGeneratedImageUrl(),
                    vr.getPhrase(),
                    feed.getCaption(),
                    feed.getIsPublic(),
                    feed.getViewCount(),
                    reactionsMap.getOrDefault(fid, Collections.emptyList()),
                    commentCountMap.getOrDefault(fid, 0L),
                    myReactionsMap.getOrDefault(fid, Collections.emptyList()),
                    feed.getCreatedAt(),
                    feed.getUpdatedAt()
            );
        }).toList();
    }

    /** 단건 변환: getFeedDetail, createFeed, updateFeed 등 단건 조회용 */
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
        return feedReactionRepository.findByFeedFeedIdAndUserUserId(feedId, userId)
                .map(r -> List.of(r.getReactionType().getValue()))
                .orElse(Collections.emptyList());
    }
}
