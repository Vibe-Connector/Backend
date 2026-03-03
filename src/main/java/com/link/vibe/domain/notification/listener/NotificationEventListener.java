package com.link.vibe.domain.notification.listener;

import com.link.vibe.domain.feed.entity.Feed;
import com.link.vibe.domain.feed.entity.FeedComment;
import com.link.vibe.domain.feed.repository.FeedCommentRepository;
import com.link.vibe.domain.feed.repository.FeedRepository;
import com.link.vibe.domain.notification.entity.NotificationType;
import com.link.vibe.domain.notification.service.NotificationService;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.global.event.CommentEvent;
import com.link.vibe.global.event.FeedReactionEvent;
import com.link.vibe.global.event.FollowEvent;
import com.link.vibe.global.event.VibeCompleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final FeedCommentRepository feedCommentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVibeComplete(VibeCompleteEvent event) {
        log.info("[Async] Vibe 완료 이벤트 수신: sessionId={}, userId={}", event.sessionId(), event.userId());
        notificationService.create(
                event.userId(),
                NotificationType.VIBE_COMPLETE,
                "Vibe 완료",
                "회원님의 Vibe 큐레이션이 완료되었습니다",
                "/vibes/sessions/" + event.sessionId(),
                event.sessionId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFollow(FollowEvent event) {
        log.info("[Async] 팔로우 이벤트 수신: follower={}, following={}", event.followerUserId(), event.followingUserId());
        String followerNickname = resolveNickname(event.followerUserId());
        notificationService.create(
                event.followingUserId(),
                NotificationType.FOLLOW,
                "새 팔로워",
                followerNickname + "님이 회원님을 팔로우했습니다",
                "/users/" + event.followerUserId(),
                event.followerUserId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedReaction(FeedReactionEvent event) {
        log.info("[Async] 피드 반응 이벤트 수신: feedId={}, actor={}", event.feedId(), event.actorUserId());
        Feed feed = feedRepository.findById(event.feedId()).orElse(null);
        if (feed == null)
            return;

        // 자기 자신의 피드에 반응한 경우 알림 건너뜀
        Long feedOwnerId = feed.getUser().getUserId();
        if (feedOwnerId.equals(event.actorUserId()))
            return;

        String actorNickname = resolveNickname(event.actorUserId());
        notificationService.create(
                feedOwnerId,
                NotificationType.FEED_REACTION,
                "피드 반응",
                actorNickname + "님이 회원님의 피드에 반응했습니다",
                "/feeds/" + event.feedId(),
                event.feedId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComment(CommentEvent event) {
        log.info("[Async] 댓글 이벤트 수신: feedId={}, commentId={}, actor={}",
                event.feedId(), event.commentId(), event.actorUserId());
        Feed feed = feedRepository.findById(event.feedId()).orElse(null);
        if (feed == null)
            return;

        String actorNickname = resolveNickname(event.actorUserId());

        // 1. 피드 작성자에게 알림 (자기 자신이 아닌 경우)
        Long feedOwnerId = feed.getUser().getUserId();
        if (!feedOwnerId.equals(event.actorUserId())) {
            notificationService.create(
                    feedOwnerId,
                    NotificationType.FEED_COMMENT,
                    "새 댓글",
                    actorNickname + "님이 댓글을 남겼습니다",
                    "/feeds/" + event.feedId(),
                    event.feedId());
        }

        // 2. 대댓글이면 부모 댓글 작성자에게도 알림
        if (event.parentCommentId() != null) {
            FeedComment parentComment = feedCommentRepository.findById(event.parentCommentId()).orElse(null);
            if (parentComment != null) {
                Long parentAuthorId = parentComment.getUser().getUserId();
                // 자기 자신에게 대댓글한 경우 & 피드 작성자와 중복 알림 방지
                if (!parentAuthorId.equals(event.actorUserId()) && !parentAuthorId.equals(feedOwnerId)) {
                    notificationService.create(
                            parentAuthorId,
                            NotificationType.FEED_COMMENT,
                            "대댓글",
                            actorNickname + "님이 회원님의 댓글에 답글을 남겼습니다",
                            "/feeds/" + event.feedId(),
                            event.feedId());
                }
            }
        }
    }

    private String resolveNickname(Long userId) {
        return userRepository.findById(userId)
                .map(User::getNickname)
                .orElse("알 수 없는 사용자");
    }
}
