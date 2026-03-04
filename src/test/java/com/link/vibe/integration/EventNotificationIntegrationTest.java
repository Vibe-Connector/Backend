package com.link.vibe.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.config.TestMailConfig;
import com.link.vibe.config.TestRedisConfig;
import com.link.vibe.config.TestS3Config;
import com.link.vibe.domain.feed.dto.CommentCreateRequest;
import com.link.vibe.domain.feed.entity.Feed;
import com.link.vibe.domain.feed.entity.FeedComment;
import com.link.vibe.domain.feed.entity.FeedReaction;
import com.link.vibe.domain.feed.entity.ReactionType;
import com.link.vibe.domain.feed.repository.FeedCommentRepository;
import com.link.vibe.domain.feed.repository.FeedReactionRepository;
import com.link.vibe.domain.feed.repository.FeedRepository;
import com.link.vibe.domain.follow.entity.Follow;
import com.link.vibe.domain.follow.repository.FollowRepository;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
import com.link.vibe.global.event.CommentEvent;
import com.link.vibe.global.event.FeedReactionEvent;
import com.link.vibe.global.event.FollowEvent;
import com.link.vibe.global.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestRedisConfig.class, TestS3Config.class, TestMailConfig.class })
@Transactional
@RecordApplicationEvents
@DisplayName("EventNotificationIntegrationTest — 이벤트 발행 검증 (Layer 2)")
class EventNotificationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ApplicationEvents events;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired EntityManager em;

    @Autowired UserRepository userRepository;
    @Autowired FeedRepository feedRepository;
    @Autowired FeedReactionRepository feedReactionRepository;
    @Autowired FeedCommentRepository feedCommentRepository;
    @Autowired FollowRepository followRepository;
    @Autowired VibeSessionRepository vibeSessionRepository;
    @Autowired VibeResultRepository vibeResultRepository;

    private User myUser;
    private User targetUser;
    private String myAccessToken;
    private Feed testFeed;

    @BeforeEach
    void setUp() {
        myUser = userRepository.save(User.builder()
                .email("event-my@test.com")
                .password("password123")
                .nickname("내유저")
                .name("My User")
                .build());

        targetUser = userRepository.save(User.builder()
                .email("event-target@test.com")
                .password("password123")
                .nickname("대상유저")
                .name("Target User")
                .build());

        myAccessToken = jwtTokenProvider.createAccessToken(
                myUser.getUserId(), myUser.getEmail());

        // 테스트용 피드 생성 (targetUser 소유)
        VibeSession session = vibeSessionRepository.save(
                VibeSession.builder().userId(targetUser.getUserId()).build());
        VibeResult result = vibeResultRepository.save(
                VibeResult.builder()
                        .vibeSession(session)
                        .phrase("test phrase")
                        .aiAnalysis("test analysis")
                        .aiModelVersion("test-v1")
                        .processingTimeMs(100)
                        .build());
        testFeed = feedRepository.save(
                Feed.create(targetUser, result, "테스트 피드", true));

        em.flush();
        em.clear();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ═══════════════════════════════════════════
    // 팔로우 이벤트 발행
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("팔로우 이벤트 발행")
    class FollowEventPublish {

        @Test
        @DisplayName("팔로우 API 호출 시 FollowEvent가 발행된다")
        void followPublishesEvent() throws Exception {
            // when
            mockMvc.perform(post("/api/v1/users/{userId}/follow", targetUser.getUserId())
                            .header("Authorization", bearer(myAccessToken)))
                    .andExpect(status().isOk());

            // then
            assertThat(events.stream(FollowEvent.class).count()).isEqualTo(1);

            FollowEvent event = events.stream(FollowEvent.class).findFirst().orElseThrow();
            assertThat(event.followerUserId()).isEqualTo(myUser.getUserId());
            assertThat(event.followingUserId()).isEqualTo(targetUser.getUserId());
        }

        @Test
        @DisplayName("언팔로우 API 호출 시 이벤트가 발행되지 않는다")
        void unfollowDoesNotPublishEvent() throws Exception {
            // given: 팔로우 관계를 직접 생성 (이벤트 없이)
            followRepository.save(Follow.builder()
                    .follower(myUser)
                    .following(targetUser)
                    .build());
            em.flush();
            em.clear();

            // when
            mockMvc.perform(delete("/api/v1/users/{userId}/follow", targetUser.getUserId())
                            .header("Authorization", bearer(myAccessToken)))
                    .andExpect(status().isOk());

            // then
            assertThat(events.stream(FollowEvent.class).count()).isEqualTo(0);
        }

        @Test
        @DisplayName("자기 자신 팔로우 시 400 에러 + 이벤트 미발행")
        void selfFollowFails() throws Exception {
            // when
            mockMvc.perform(post("/api/v1/users/{userId}/follow", myUser.getUserId())
                            .header("Authorization", bearer(myAccessToken)))
                    .andExpect(status().isBadRequest());

            // then
            assertThat(events.stream(FollowEvent.class).count()).isEqualTo(0);
        }
    }

    // ═══════════════════════════════════════════
    // 피드 반응 이벤트 발행
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("피드 반응 이벤트 발행")
    class FeedReactionEventPublish {

        @Test
        @DisplayName("피드 반응 추가 시 FeedReactionEvent가 발행된다")
        void reactionAddPublishesEvent() throws Exception {
            // when (첫 반응 → 추가)
            mockMvc.perform(post("/api/v1/feeds/{feedId}/reactions", testFeed.getFeedId())
                            .header("Authorization", bearer(myAccessToken))
                            .param("reactionType", "LIKE"))
                    .andExpect(status().isOk());

            // then
            assertThat(events.stream(FeedReactionEvent.class).count()).isEqualTo(1);

            FeedReactionEvent event = events.stream(FeedReactionEvent.class)
                    .findFirst().orElseThrow();
            assertThat(event.feedId()).isEqualTo(testFeed.getFeedId());
            assertThat(event.actorUserId()).isEqualTo(myUser.getUserId());
            assertThat(event.reactionType()).isEqualTo("LIKE");
        }

        @Test
        @DisplayName("피드 반응 취소 (토글) 시 이벤트가 발행되지 않는다")
        void reactionCancelDoesNotPublishEvent() throws Exception {
            // given: 반응을 직접 생성 (이벤트 없이)
            Feed feed = feedRepository.findById(testFeed.getFeedId()).orElseThrow();
            User user = userRepository.findById(myUser.getUserId()).orElseThrow();
            feedReactionRepository.save(FeedReaction.create(feed, user, ReactionType.LIKE));
            em.flush();
            em.clear();

            // when (같은 반응 재요청 → 삭제)
            mockMvc.perform(post("/api/v1/feeds/{feedId}/reactions", testFeed.getFeedId())
                            .header("Authorization", bearer(myAccessToken))
                            .param("reactionType", "LIKE"))
                    .andExpect(status().isOk());

            // then
            assertThat(events.stream(FeedReactionEvent.class).count()).isEqualTo(0);
        }
    }

    // ═══════════════════════════════════════════
    // 댓글 이벤트 발행
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("댓글 이벤트 발행")
    class CommentEventPublish {

        @Test
        @DisplayName("댓글 작성 시 CommentEvent가 발행된다 (parentCommentId = null)")
        void commentPublishesEvent() throws Exception {
            // when
            CommentCreateRequest request = new CommentCreateRequest("테스트 댓글입니다", null);

            mockMvc.perform(post("/api/v1/feeds/{feedId}/comments", testFeed.getFeedId())
                            .header("Authorization", bearer(myAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // then
            assertThat(events.stream(CommentEvent.class).count()).isEqualTo(1);

            CommentEvent event = events.stream(CommentEvent.class).findFirst().orElseThrow();
            assertThat(event.feedId()).isEqualTo(testFeed.getFeedId());
            assertThat(event.actorUserId()).isEqualTo(myUser.getUserId());
            assertThat(event.parentCommentId()).isNull();
            assertThat(event.commentId()).isNotNull();
        }

        @Test
        @DisplayName("대댓글 작성 시 CommentEvent에 parentCommentId가 포함된다")
        void replyPublishesEventWithParentId() throws Exception {
            // given: 부모 댓글 생성
            Feed feed = feedRepository.findById(testFeed.getFeedId()).orElseThrow();
            User user = userRepository.findById(targetUser.getUserId()).orElseThrow();
            FeedComment parentComment = feedCommentRepository.save(
                    FeedComment.create(feed, user, null, "부모 댓글"));
            em.flush();
            em.clear();

            // when
            CommentCreateRequest request = new CommentCreateRequest(
                    "대댓글입니다", parentComment.getCommentId());

            mockMvc.perform(post("/api/v1/feeds/{feedId}/comments", testFeed.getFeedId())
                            .header("Authorization", bearer(myAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // then
            assertThat(events.stream(CommentEvent.class).count()).isEqualTo(1);

            CommentEvent event = events.stream(CommentEvent.class).findFirst().orElseThrow();
            assertThat(event.feedId()).isEqualTo(testFeed.getFeedId());
            assertThat(event.actorUserId()).isEqualTo(myUser.getUserId());
            assertThat(event.parentCommentId()).isEqualTo(parentComment.getCommentId());
        }
    }
}
