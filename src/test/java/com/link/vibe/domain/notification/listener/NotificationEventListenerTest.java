package com.link.vibe.domain.notification.listener;

import com.link.vibe.config.TestMailConfig;
import com.link.vibe.config.TestRedisConfig;
import com.link.vibe.config.TestS3Config;
import com.link.vibe.domain.feed.entity.Feed;
import com.link.vibe.domain.feed.entity.FeedComment;
import com.link.vibe.domain.feed.repository.FeedCommentRepository;
import com.link.vibe.domain.feed.repository.FeedRepository;
import com.link.vibe.domain.notification.entity.Notification;
import com.link.vibe.domain.notification.entity.NotificationType;
import com.link.vibe.domain.notification.repository.NotificationRepository;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
import com.link.vibe.global.event.CommentEvent;
import com.link.vibe.global.event.FeedReactionEvent;
import com.link.vibe.global.event.FollowEvent;
import com.link.vibe.global.event.VibeCompleteEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({ TestRedisConfig.class, TestS3Config.class, TestMailConfig.class })
@Transactional
@DisplayName("NotificationEventListener — 리스너 단위 테스트 (Layer 1)")
class NotificationEventListenerTest {

    @Autowired NotificationEventListener listenerProxy;

    /** @Async AOP 프록시를 우회하여 동기 실행하기 위한 원본 빈 */
    private NotificationEventListener listener;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;
    @Autowired FeedRepository feedRepository;
    @Autowired FeedCommentRepository feedCommentRepository;
    @Autowired VibeSessionRepository vibeSessionRepository;
    @Autowired VibeResultRepository vibeResultRepository;
    @Autowired EntityManager em;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // @Async 프록시를 우회하여 리스너 메서드를 동기 실행
        listener = AopTestUtils.getUltimateTargetObject(listenerProxy);

        userA = userRepository.save(User.builder()
                .email("listener-userA@test.com")
                .password("password123")
                .nickname("유저A")
                .name("User A")
                .build());

        userB = userRepository.save(User.builder()
                .email("listener-userB@test.com")
                .password("password123")
                .nickname("유저B")
                .name("User B")
                .build());

        userC = userRepository.save(User.builder()
                .email("listener-userC@test.com")
                .password("password123")
                .nickname("유저C")
                .name("User C")
                .build());

        em.flush();
        em.clear();
    }

    // ── 헬퍼 메서드 ──

    private Feed createFeed(User owner) {
        VibeSession session = vibeSessionRepository.save(
                VibeSession.builder().userId(owner.getUserId()).build());
        VibeResult result = vibeResultRepository.save(
                VibeResult.builder()
                        .vibeSession(session)
                        .phrase("test phrase")
                        .aiAnalysis("test analysis")
                        .aiModelVersion("test-v1")
                        .processingTimeMs(100)
                        .build());
        return feedRepository.save(Feed.create(owner, result, "테스트 피드", true));
    }

    private FeedComment createComment(Feed feed, User author, FeedComment parent) {
        return feedCommentRepository.save(
                FeedComment.create(feed, author, parent, "테스트 댓글"));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private List<Notification> findNotificationsForUser(Long userId) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getUserId().equals(userId))
                .toList();
    }

    // ═══════════════════════════════════════════
    // FollowEvent 알림
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("FollowEvent 알림")
    class FollowEventNotification {

        @Test
        @DisplayName("팔로우 시 팔로잉 대상에게 FOLLOW 알림이 생성된다")
        void followCreatesNotification() {
            // when
            listener.onFollow(new FollowEvent(userA.getUserId(), userB.getUserId()));
            flushAndClear();

            // then
            List<Notification> notifications = findNotificationsForUser(userB.getUserId());
            assertThat(notifications).hasSize(1);

            Notification noti = notifications.get(0);
            assertThat(noti.getType()).isEqualTo(NotificationType.FOLLOW);
            assertThat(noti.getTitle()).isEqualTo("새 팔로워");
            assertThat(noti.getBody()).contains("유저A");
        }

        @Test
        @DisplayName("팔로우 알림의 linkUrl이 /users/{followerUserId} 형식이다")
        void followNotificationLinkUrl() {
            // when
            listener.onFollow(new FollowEvent(userA.getUserId(), userB.getUserId()));
            flushAndClear();

            // then
            Notification noti = findNotificationsForUser(userB.getUserId()).get(0);
            assertThat(noti.getLinkUrl()).isEqualTo("/users/" + userA.getUserId());
        }

        @Test
        @DisplayName("팔로우 알림의 referenceId가 followerUserId이다")
        void followNotificationReferenceId() {
            // when
            listener.onFollow(new FollowEvent(userA.getUserId(), userB.getUserId()));
            flushAndClear();

            // then
            Notification noti = findNotificationsForUser(userB.getUserId()).get(0);
            assertThat(noti.getReferenceId()).isEqualTo(userA.getUserId());
        }
    }

    // ═══════════════════════════════════════════
    // FeedReactionEvent 알림
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("FeedReactionEvent 알림")
    class FeedReactionEventNotification {

        @Test
        @DisplayName("타인 피드에 반응 시 피드 작성자에게 FEED_REACTION 알림이 생성된다")
        void reactionCreatesNotificationForFeedOwner() {
            // given
            Feed feed = createFeed(userA);
            flushAndClear();

            // when
            listener.onFeedReaction(
                    new FeedReactionEvent(feed.getFeedId(), userB.getUserId(), "LIKE"));
            flushAndClear();

            // then
            List<Notification> notifications = findNotificationsForUser(userA.getUserId());
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.FEED_REACTION);
        }

        @Test
        @DisplayName("본인 피드에 본인이 반응 시 알림이 생성되지 않는다")
        void selfReactionDoesNotCreateNotification() {
            // given
            Feed feed = createFeed(userA);
            flushAndClear();

            // when
            listener.onFeedReaction(
                    new FeedReactionEvent(feed.getFeedId(), userA.getUserId(), "LIKE"));
            flushAndClear();

            // then
            List<Notification> all = notificationRepository.findAll();
            assertThat(all).isEmpty();
        }

        @Test
        @DisplayName("반응 알림 body에 반응한 사용자의 닉네임이 포함된다")
        void reactionNotificationBodyContainsNickname() {
            // given
            Feed feed = createFeed(userA);
            flushAndClear();

            // when
            listener.onFeedReaction(
                    new FeedReactionEvent(feed.getFeedId(), userB.getUserId(), "LOVE"));
            flushAndClear();

            // then
            Notification noti = findNotificationsForUser(userA.getUserId()).get(0);
            assertThat(noti.getBody()).contains("유저B");
        }
    }

    // ═══════════════════════════════════════════
    // CommentEvent 알림
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("CommentEvent 알림")
    class CommentEventNotification {

        @Test
        @DisplayName("타인 피드에 댓글 시 피드 작성자에게 FEED_COMMENT 알림이 생성된다")
        void commentCreatesNotificationForFeedOwner() {
            // given
            Feed feed = createFeed(userA);
            FeedComment comment = createComment(feed, userB, null);
            flushAndClear();

            // when
            listener.onComment(new CommentEvent(
                    feed.getFeedId(), comment.getCommentId(),
                    userB.getUserId(), null));
            flushAndClear();

            // then
            List<Notification> notifications = findNotificationsForUser(userA.getUserId());
            assertThat(notifications).hasSize(1);

            Notification noti = notifications.get(0);
            assertThat(noti.getType()).isEqualTo(NotificationType.FEED_COMMENT);
            assertThat(noti.getTitle()).isEqualTo("새 댓글");
        }

        @Test
        @DisplayName("본인 피드에 본인이 댓글 시 알림이 생성되지 않는다")
        void selfCommentDoesNotCreateNotification() {
            // given
            Feed feed = createFeed(userA);
            FeedComment comment = createComment(feed, userA, null);
            flushAndClear();

            // when
            listener.onComment(new CommentEvent(
                    feed.getFeedId(), comment.getCommentId(),
                    userA.getUserId(), null));
            flushAndClear();

            // then
            List<Notification> all = notificationRepository.findAll();
            assertThat(all).isEmpty();
        }

        @Test
        @DisplayName("대댓글 시 피드 작성자와 부모 댓글 작성자 모두에게 알림이 생성된다")
        void replyCreatesNotificationForBothOwners() {
            // given: userA의 피드, userB의 댓글, userC가 대댓글
            Feed feed = createFeed(userA);
            FeedComment parentComment = createComment(feed, userB, null);
            FeedComment reply = createComment(feed, userC, parentComment);
            flushAndClear();

            // when
            listener.onComment(new CommentEvent(
                    feed.getFeedId(), reply.getCommentId(),
                    userC.getUserId(), parentComment.getCommentId()));
            flushAndClear();

            // then
            List<Notification> forFeedOwner = findNotificationsForUser(userA.getUserId());
            assertThat(forFeedOwner).hasSize(1);
            assertThat(forFeedOwner.get(0).getTitle()).isEqualTo("새 댓글");

            List<Notification> forParentAuthor = findNotificationsForUser(userB.getUserId());
            assertThat(forParentAuthor).hasSize(1);
            assertThat(forParentAuthor.get(0).getTitle()).isEqualTo("대댓글");

            // 전체 알림 수 = 2
            assertThat(notificationRepository.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("대댓글 — 부모 댓글 작성자가 피드 작성자와 같으면 중복 알림이 방지된다")
        void replyDeduplicatesWhenParentAuthorIsFeedOwner() {
            // given: userA의 피드, userA의 댓글, userB가 대댓글
            Feed feed = createFeed(userA);
            FeedComment parentComment = createComment(feed, userA, null);
            FeedComment reply = createComment(feed, userB, parentComment);
            flushAndClear();

            // when
            listener.onComment(new CommentEvent(
                    feed.getFeedId(), reply.getCommentId(),
                    userB.getUserId(), parentComment.getCommentId()));
            flushAndClear();

            // then: userA에게 알림 1건만 (피드 작성자 알림만, 부모 댓글 알림은 중복이므로 미생성)
            List<Notification> forUserA = findNotificationsForUser(userA.getUserId());
            assertThat(forUserA).hasSize(1);
            assertThat(forUserA.get(0).getTitle()).isEqualTo("새 댓글");

            assertThat(notificationRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("대댓글 — 본인 댓글에 본인이 대댓글 시 피드 작성자에게만 알림이 생성된다")
        void selfReplyOnlyNotifiesFeedOwner() {
            // given: userA의 피드, userB의 댓글, userB가 자기 댓글에 대댓글
            Feed feed = createFeed(userA);
            FeedComment parentComment = createComment(feed, userB, null);
            FeedComment reply = createComment(feed, userB, parentComment);
            flushAndClear();

            // when
            listener.onComment(new CommentEvent(
                    feed.getFeedId(), reply.getCommentId(),
                    userB.getUserId(), parentComment.getCommentId()));
            flushAndClear();

            // then: userA에게만 알림 1건 (피드 작성자), userB 본인에게는 미생성
            List<Notification> forFeedOwner = findNotificationsForUser(userA.getUserId());
            assertThat(forFeedOwner).hasSize(1);

            List<Notification> forSelf = findNotificationsForUser(userB.getUserId());
            assertThat(forSelf).isEmpty();

            assertThat(notificationRepository.findAll()).hasSize(1);
        }
    }

    // ═══════════════════════════════════════════
    // VibeCompleteEvent 알림
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("VibeCompleteEvent 알림")
    class VibeCompleteEventNotification {

        @Test
        @DisplayName("Vibe 완료 시 본인에게 VIBE_COMPLETE 알림이 생성된다")
        void vibeCompleteCreatesNotification() {
            // given
            VibeSession session = vibeSessionRepository.save(
                    VibeSession.builder().userId(userA.getUserId()).build());
            flushAndClear();

            // when
            listener.onVibeComplete(new VibeCompleteEvent(session.getSessionId(), userA.getUserId()));
            flushAndClear();

            // then
            List<Notification> notifications = findNotificationsForUser(userA.getUserId());
            assertThat(notifications).hasSize(1);

            Notification noti = notifications.get(0);
            assertThat(noti.getType()).isEqualTo(NotificationType.VIBE_COMPLETE);
            assertThat(noti.getTitle()).isEqualTo("Vibe 완료");
        }

        @Test
        @DisplayName("Vibe 완료 알림의 linkUrl이 /vibes/sessions/{sessionId} 형식이다")
        void vibeCompleteNotificationLinkUrl() {
            // given
            VibeSession session = vibeSessionRepository.save(
                    VibeSession.builder().userId(userA.getUserId()).build());
            flushAndClear();

            // when
            listener.onVibeComplete(new VibeCompleteEvent(session.getSessionId(), userA.getUserId()));
            flushAndClear();

            // then
            Notification noti = findNotificationsForUser(userA.getUserId()).get(0);
            assertThat(noti.getLinkUrl()).isEqualTo("/vibes/sessions/" + session.getSessionId());
        }
    }

    // ═══════════════════════════════════════════
    // 엣지 케이스
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("존재하지 않는 피드에 대한 FeedReactionEvent 발생 시 예외 없이 무시된다")
        void nonExistentFeedReactionIsIgnored() {
            // given
            Long nonExistentFeedId = 99999L;

            // when & then — 예외 없이 정상 종료
            listener.onFeedReaction(
                    new FeedReactionEvent(nonExistentFeedId, userA.getUserId(), "LIKE"));
            flushAndClear();

            List<Notification> all = notificationRepository.findAll();
            assertThat(all).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 FollowEvent 발생 시 닉네임이 '알 수 없는 사용자'로 대체된다")
        void nonExistentUserNicknameIsReplaced() {
            // given: 존재하지 않는 followerUserId
            Long nonExistentUserId = 99999L;

            // when
            listener.onFollow(new FollowEvent(nonExistentUserId, userB.getUserId()));
            flushAndClear();

            // then
            List<Notification> notifications = findNotificationsForUser(userB.getUserId());
            assertThat(notifications).hasSize(1);
            assertThat(notifications.get(0).getBody()).contains("알 수 없는 사용자");
        }
    }
}
