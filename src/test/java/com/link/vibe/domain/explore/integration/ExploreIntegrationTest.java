package com.link.vibe.domain.explore.integration;

import com.link.vibe.config.TestMailConfig;
import com.link.vibe.config.TestRedisConfig;
import com.link.vibe.config.TestS3Config;
import com.link.vibe.domain.feed.entity.Feed;
import com.link.vibe.domain.feed.entity.FeedComment;
import com.link.vibe.domain.feed.entity.FeedReaction;
import com.link.vibe.domain.feed.entity.ReactionType;
import com.link.vibe.domain.feed.repository.FeedCommentRepository;
import com.link.vibe.domain.feed.repository.FeedReactionRepository;
import com.link.vibe.domain.feed.repository.FeedRepository;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestRedisConfig.class, TestS3Config.class, TestMailConfig.class })
@Transactional
class ExploreIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager em;

    @Autowired UserRepository userRepository;
    @Autowired FeedRepository feedRepository;
    @Autowired FeedReactionRepository feedReactionRepository;
    @Autowired FeedCommentRepository feedCommentRepository;
    @Autowired VibeSessionRepository vibeSessionRepository;
    @Autowired VibeResultRepository vibeResultRepository;

    private User author;
    private User otherUser;
    private User otherUser2;

    @BeforeEach
    void setUp() {
        author = userRepository.save(User.builder()
                .email("explore-author@example.com")
                .password("password123")
                .nickname("author")
                .name("Author")
                .build());

        otherUser = userRepository.save(User.builder()
                .email("explore-other@example.com")
                .password("password123")
                .nickname("otheruser")
                .name("Other User")
                .build());

        otherUser2 = userRepository.save(User.builder()
                .email("explore-other2@example.com")
                .password("password123")
                .nickname("otheruser2")
                .name("Other User 2")
                .build());
    }

    private Feed createPublicFeed(User user, String caption, int viewCount) {
        VibeSession session = vibeSessionRepository.save(
                VibeSession.builder().userId(user.getUserId()).build());
        VibeResult result = vibeResultRepository.save(
                VibeResult.builder()
                        .vibeSession(session)
                        .phrase("test phrase")
                        .aiAnalysis("test analysis")
                        .aiModelVersion("test-v1")
                        .processingTimeMs(100)
                        .build());
        Feed feed = Feed.create(user, result, caption, true);
        // viewCount를 설정하기 위해 incrementViewCount 반복
        for (int i = 0; i < viewCount; i++) {
            feed.incrementViewCount();
        }
        return feedRepository.save(feed);
    }

    private Feed createPrivateFeed(User user, String caption) {
        VibeSession session = vibeSessionRepository.save(
                VibeSession.builder().userId(user.getUserId()).build());
        VibeResult result = vibeResultRepository.save(
                VibeResult.builder()
                        .vibeSession(session)
                        .phrase("test phrase")
                        .aiAnalysis("test analysis")
                        .aiModelVersion("test-v1")
                        .processingTimeMs(100)
                        .build());
        return feedRepository.save(Feed.create(user, result, caption, false));
    }

    private void addReactions(Feed feed, User user, int count) {
        // 한 유저 한 피드당 하나의 반응만 가능 (uk_feed_user 제약)
        // count > 1이면 다른 유저로 분산 생성
        User[] reactors = { user, otherUser2 };
        ReactionType[] types = ReactionType.values();
        for (int i = 0; i < count; i++) {
            feedReactionRepository.save(
                    FeedReaction.create(feed, reactors[i % reactors.length], types[i % types.length]));
        }
    }

    private void addComment(Feed feed, User commenter, String content) {
        feedCommentRepository.save(FeedComment.create(feed, commenter, null, content));
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // ═══════════════════════════════════════════
    // GET /api/v1/explore/vibes — 인기 Vibe 탐색
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/explore/vibes - 인기 Vibe 탐색")
    class GetPopularVibes {

        @Test
        @DisplayName("인기 Vibe 목록을 조회할 수 있다")
        void success() throws Exception {
            createPublicFeed(author, "피드1", 10);
            createPublicFeed(author, "피드2", 20);
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("빈 결과를 조회할 수 있다")
        void empty() throws Exception {
            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("인기도 순으로 정렬된다 (높은 점수가 먼저)")
        void orderedByPopularity() throws Exception {
            // viewCount=10, 반응 0, 댓글 0 → 점수 = 10
            createPublicFeed(author, "인기낮음", 10);

            // feedB: viewCount=10, 반응 2(otherUser), 댓글 1(otherUser) → engagement=3
            //   점수 = 3*10 + 10 + 3*1000/(10+10) = 30 + 10 + 150 = 190
            Feed feedB = createPublicFeed(author, "인기높음", 10);
            addReactions(feedB, otherUser, 2);
            addComment(feedB, otherUser, "좋은 글!");
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].caption").value("인기높음"))
                    .andExpect(jsonPath("$.data.content[1].caption").value("인기낮음"));
        }

        @Test
        @DisplayName("비공개 피드는 탐색 결과에 포함되지 않는다")
        void excludesPrivateFeeds() throws Exception {
            createPublicFeed(author, "공개피드", 10);
            createPrivateFeed(author, "비공개피드");
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].caption").value("공개피드"));
        }

        @Test
        @DisplayName("작성자 본인 댓글은 댓글수에 포함되지 않는다")
        void excludesSelfComments() throws Exception {
            Feed feed = createPublicFeed(author, "테스트", 10);
            addComment(feed, author, "본인 댓글");       // 본인 → 제외
            addComment(feed, otherUser, "타인 댓글");     // 타인 → 포함
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].commentCount").value(1));
        }

        @Test
        @DisplayName("응답에 필수 필드가 모두 포함된다")
        void responseContainsAllFields() throws Exception {
            Feed feed = createPublicFeed(author, "필드확인", 5);
            addReactions(feed, otherUser, 1);
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].feedId").isNumber())
                    .andExpect(jsonPath("$.data.content[0].generatedImageUrl").hasJsonPath())
                    .andExpect(jsonPath("$.data.content[0].caption").value("필드확인"))
                    .andExpect(jsonPath("$.data.content[0].authorId").value(author.getUserId()))
                    .andExpect(jsonPath("$.data.content[0].authorNickname").value("author"))
                    .andExpect(jsonPath("$.data.content[0].viewCount").value(5))
                    .andExpect(jsonPath("$.data.content[0].reactionCount").value(1))
                    .andExpect(jsonPath("$.data.content[0].commentCount").value(0))
                    .andExpect(jsonPath("$.data.content[0].popularityScore").isNumber())
                    .andExpect(jsonPath("$.data.content[0].createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("인증 없이 조회할 수 있다 (공개 API)")
        void noAuthRequired() throws Exception {
            createPublicFeed(author, "공개탐색", 0);
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ═══════════════════════════════════════════
    // 기간 필터 테스트
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("기간 필터 테스트")
    class PeriodFilter {

        @Test
        @DisplayName("기본 기간은 WEEK이다")
        void defaultPeriodIsWeek() throws Exception {
            createPublicFeed(author, "최근피드", 0);
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }

        @Test
        @DisplayName("DAY 필터로 조회할 수 있다")
        void dayPeriod() throws Exception {
            createPublicFeed(author, "오늘피드", 0);
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "DAY")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("MONTH 필터로 조회할 수 있다")
        void monthPeriod() throws Exception {
            createPublicFeed(author, "이번달피드", 0);
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ═══════════════════════════════════════════
    // 커서 페이지네이션 테스트
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("커서 페이지네이션 테스트")
    class CursorPagination {

        @Test
        @DisplayName("커서 페이지네이션이 동작한다")
        void cursorPagination() throws Exception {
            // 3개 피드 생성 (viewCount로 점수 차별화)
            createPublicFeed(author, "피드1", 30);
            createPublicFeed(author, "피드2", 20);
            createPublicFeed(author, "피드3", 10);
            flushAndClear();

            // size=2 → 상위 2개만, hasNext=true
            String response = mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                    .andReturn().getResponse().getContentAsString();

            // nextCursor 추출하여 2페이지 요청
            String nextCursor = com.jayway.jsonpath.JsonPath.read(response, "$.data.nextCursor");

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "2")
                    .param("cursor", nextCursor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("size 파라미터가 적용된다")
        void sizeParam() throws Exception {
            createPublicFeed(author, "피드1", 10);
            createPublicFeed(author, "피드2", 20);
            createPublicFeed(author, "피드3", 30);
            flushAndClear();

            mockMvc.perform(get("/api/v1/explore/vibes")
                    .param("period", "MONTH")
                    .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(true));
        }
    }

    // ═══════════════════════════════════════════
    // 데이터 정합성 테스트
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("데이터 정합성 테스트")
    class DataConsistency {

        @Test
        @DisplayName("피드 삭제(soft delete) 후 Explore 결과에서 제외된다")
        void deletedFeedExcludedFromExplore() throws Exception {
            // given
            Feed feed = createPublicFeed(author, "삭제될피드", 10);
            flushAndClear();

            // 삭제 전 — Explore에 노출
            mockMvc.perform(get("/api/v1/explore/vibes")
                            .param("period", "MONTH")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));

            // when: soft delete
            Feed managedFeed = feedRepository.findById(feed.getFeedId()).orElseThrow();
            managedFeed.softDelete();
            flushAndClear();

            // then: Explore에서 제외
            mockMvc.perform(get("/api/v1/explore/vibes")
                            .param("period", "MONTH")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }

        @Test
        @DisplayName("비공개 전환 후 Explore 결과에서 제외된다")
        void privateFeedExcludedFromExplore() throws Exception {
            // given
            Feed feed = createPublicFeed(author, "비공개전환피드", 10);
            flushAndClear();

            // 전환 전 — Explore에 노출
            mockMvc.perform(get("/api/v1/explore/vibes")
                            .param("period", "MONTH")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));

            // when: 비공개 전환
            Feed managedFeed = feedRepository.findById(feed.getFeedId()).orElseThrow();
            managedFeed.update(managedFeed.getCaption(), false);
            flushAndClear();

            // then: Explore에서 제외
            mockMvc.perform(get("/api/v1/explore/vibes")
                            .param("period", "MONTH")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0));
        }

        @Test
        @DisplayName("닉네임 변경 후 Explore 조회 시 새 닉네임이 반영된다")
        void nicknameChangeReflectedInExplore() throws Exception {
            // given
            createPublicFeed(author, "닉네임테스트", 10);
            flushAndClear();

            // 변경 전 — 기존 닉네임
            mockMvc.perform(get("/api/v1/explore/vibes")
                            .param("period", "MONTH")
                            .param("size", "20"))
                    .andExpect(jsonPath("$.data.content[0].authorNickname").value("author"));

            // when: 닉네임 변경
            em.createQuery("UPDATE User u SET u.nickname = :nickname WHERE u.userId = :userId")
                    .setParameter("nickname", "변경된닉네임")
                    .setParameter("userId", author.getUserId())
                    .executeUpdate();
            flushAndClear();

            // then: 새 닉네임 반영
            mockMvc.perform(get("/api/v1/explore/vibes")
                            .param("period", "MONTH")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].authorNickname").value("변경된닉네임"));
        }

        @Test
        @DisplayName("닉네임 변경 후 피드 상세 조회 시 새 닉네임이 반영된다")
        void nicknameChangeReflectedInFeedDetail() throws Exception {
            // given
            Feed feed = createPublicFeed(author, "피드상세테스트", 5);
            flushAndClear();

            // 변경 전 — 기존 닉네임
            mockMvc.perform(get("/api/v1/feeds/{feedId}", feed.getFeedId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("author"));

            // when: 닉네임 변경
            em.createQuery("UPDATE User u SET u.nickname = :nickname WHERE u.userId = :userId")
                    .setParameter("nickname", "새닉네임")
                    .setParameter("userId", author.getUserId())
                    .executeUpdate();
            flushAndClear();

            // then: 새 닉네임 반영
            mockMvc.perform(get("/api/v1/feeds/{feedId}", feed.getFeedId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
        }
    }
}
