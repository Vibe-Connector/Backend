package com.link.vibe.domain.feed.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.config.TestRedisConfig;
import com.link.vibe.config.TestS3Config;
import com.link.vibe.domain.feed.dto.CommentCreateRequest;
import com.link.vibe.domain.feed.dto.CommentUpdateRequest;
import com.link.vibe.domain.feed.dto.FeedCreateRequest;
import com.link.vibe.domain.feed.dto.FeedUpdateRequest;
import com.link.vibe.domain.feed.entity.*;
import com.link.vibe.domain.feed.repository.*;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestRedisConfig.class, TestS3Config.class})
@Transactional
class FeedIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EntityManager em;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Autowired UserRepository userRepository;
    @Autowired VibeSessionRepository vibeSessionRepository;
    @Autowired VibeResultRepository vibeResultRepository;
    @Autowired FeedRepository feedRepository;
    @Autowired FeedReactionRepository feedReactionRepository;
    @Autowired FeedCommentRepository feedCommentRepository;
    @Autowired CommentReactionRepository commentReactionRepository;

    private User testUser;
    private User otherUser;
    private VibeResult vibeResult;
    private VibeResult vibeResult2;
    private String accessToken;
    private String otherAccessToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("testuser")
                .name("Test User")
                .build());

        otherUser = userRepository.save(User.builder()
                .email("other@example.com")
                .password("password123")
                .nickname("otheruser")
                .name("Other User")
                .build());

        VibeSession session1 = vibeSessionRepository.save(
                VibeSession.builder().userId(testUser.getUserId()).build());
        vibeResult = vibeResultRepository.save(VibeResult.builder()
                .vibeSession(session1)
                .phrase("오늘의 분위기")
                .aiAnalysis("차분하고 편안한 분위기")
                .aiModelVersion("gpt-4o-mini")
                .processingTimeMs(1200)
                .build());

        VibeSession session2 = vibeSessionRepository.save(
                VibeSession.builder().userId(testUser.getUserId()).build());
        vibeResult2 = vibeResultRepository.save(VibeResult.builder()
                .vibeSession(session2)
                .phrase("두 번째 분위기")
                .aiAnalysis("활기찬 분위기")
                .aiModelVersion("gpt-4o-mini")
                .processingTimeMs(800)
                .build());

        accessToken = jwtTokenProvider.createAccessToken(
                testUser.getUserId(), testUser.getEmail());
        otherAccessToken = jwtTokenProvider.createAccessToken(
                otherUser.getUserId(), otherUser.getEmail());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Feed createFeed(User user, VibeResult vr, String caption, boolean isPublic) {
        Feed feed = Feed.create(user, vr, caption, isPublic);
        return feedRepository.save(feed);
    }

    private FeedComment createComment(Feed feed, User user, FeedComment parent, String content) {
        FeedComment comment = FeedComment.create(feed, user, parent, content);
        return feedCommentRepository.save(comment);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // ═══════════════════════════════════════════
    // 피드 CRUD
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/feeds - 피드 작성")
    class CreateFeed {

        @Test
        @DisplayName("인증된 사용자가 피드를 작성할 수 있다")
        void success() throws Exception {
            var request = new FeedCreateRequest(vibeResult.getResultId(), "나의 첫 피드", true);

            mockMvc.perform(post("/api/v1/feeds")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.caption").value("나의 첫 피드"))
                    .andExpect(jsonPath("$.data.isPublic").value(true))
                    .andExpect(jsonPath("$.data.phrase").value("오늘의 분위기"))
                    .andExpect(jsonPath("$.data.userId").value(testUser.getUserId()))
                    .andExpect(jsonPath("$.data.nickname").value("testuser"));

            assertThat(feedRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("미인증 사용자는 피드를 작성할 수 없다 (401)")
        void unauthorized() throws Exception {
            var request = new FeedCreateRequest(vibeResult.getResultId(), "캡션", true);

            mockMvc.perform(post("/api/v1/feeds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("같은 VibeResult로 중복 피드를 생성하면 409 에러")
        void duplicateResult() throws Exception {
            createFeed(testUser, vibeResult, "첫 번째", true);
            flushAndClear();

            var request = new FeedCreateRequest(vibeResult.getResultId(), "두 번째 시도", true);

            mockMvc.perform(post("/api/v1/feeds")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("FEED_002"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/feeds/{feedId} - 피드 상세 조회")
    class GetFeedDetail {

        @Test
        @DisplayName("인증된 사용자가 피드를 조회하면 내 반응 정보도 포함된다")
        void authenticatedWithMyReactions() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "테스트 피드", true);
            feedReactionRepository.save(FeedReaction.create(feed, otherUser, ReactionType.LIKE));
            flushAndClear();

            mockMvc.perform(get("/api/v1/feeds/{feedId}", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.feedId").value(feed.getFeedId()))
                    .andExpect(jsonPath("$.data.caption").value("테스트 피드"))
                    .andExpect(jsonPath("$.data.nickname").value("testuser"))
                    .andExpect(jsonPath("$.data.phrase").value("오늘의 분위기"))
                    .andExpect(jsonPath("$.data.myReactionTypes[0]").value("LIKE"));
        }

        @Test
        @DisplayName("비인증 사용자도 공개 피드를 조회할 수 있다")
        void unauthenticated() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "공개 피드", true);
            flushAndClear();

            mockMvc.perform(get("/api/v1/feeds/{feedId}", feed.getFeedId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.feedId").value(feed.getFeedId()))
                    .andExpect(jsonPath("$.data.myReactionTypes").isEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 피드 조회 시 404")
        void notFound() throws Exception {
            mockMvc.perform(get("/api/v1/feeds/{feedId}", 99999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("FEED_001"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/feeds/{feedId} - 피드 수정")
    class UpdateFeed {

        @Test
        @DisplayName("작성자가 피드를 수정할 수 있다")
        void success() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "원래 캡션", false);
            flushAndClear();

            var request = new FeedUpdateRequest("수정된 캡션", true);

            mockMvc.perform(put("/api/v1/feeds/{feedId}", feed.getFeedId())
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.caption").value("수정된 캡션"))
                    .andExpect(jsonPath("$.data.isPublic").value(true));
        }

        @Test
        @DisplayName("작성자가 아닌 사용자는 수정할 수 없다 (403)")
        void notOwner() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "캡션", true);
            flushAndClear();

            var request = new FeedUpdateRequest("해킹 시도", null);

            mockMvc.perform(put("/api/v1/feeds/{feedId}", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_002"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/feeds/{feedId} - 피드 삭제")
    class DeleteFeed {

        @Test
        @DisplayName("작성자가 피드를 삭제할 수 있다 (soft delete)")
        void success() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "삭제할 피드", true);
            Long feedId = feed.getFeedId();
            flushAndClear();

            mockMvc.perform(delete("/api/v1/feeds/{feedId}", feedId)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // soft delete 후 @SQLRestriction에 의해 조회 불가 확인
            flushAndClear();
            assertThat(feedRepository.findById(feedId)).isEmpty();
        }

        @Test
        @DisplayName("작성자가 아닌 사용자는 삭제할 수 없다 (403)")
        void notOwner() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "캡션", true);
            flushAndClear();

            mockMvc.perform(delete("/api/v1/feeds/{feedId}", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/feeds - 피드 타임라인")
    class GetFeedTimeline {

        @Test
        @DisplayName("공개 피드만 타임라인에 노출된다")
        void onlyPublicFeeds() throws Exception {
            createFeed(testUser, vibeResult, "공개 피드", true);
            createFeed(testUser, vibeResult2, "비공개 피드", false);
            flushAndClear();

            mockMvc.perform(get("/api/v1/feeds")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].caption").value("공개 피드"));
        }

        @Test
        @DisplayName("커서 기반 페이지네이션이 동작한다")
        void cursorPagination() throws Exception {
            createFeed(testUser, vibeResult, "피드1", true);
            Feed feed2 = createFeed(testUser, vibeResult2, "피드2", true);
            flushAndClear();

            // size=1 → 최신 1개만 + hasNext=true
            mockMvc.perform(get("/api/v1/feeds")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.content[0].caption").value("피드2"));

            // cursor=feed2 → 다음 페이지
            mockMvc.perform(get("/api/v1/feeds")
                            .param("size", "1")
                            .param("cursor", String.valueOf(feed2.getFeedId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.content[0].caption").value("피드1"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/feeds - 사용자 피드 목록")
    class GetUserFeeds {

        @Test
        @DisplayName("본인은 비공개 피드도 볼 수 있다")
        void ownerSeesAll() throws Exception {
            createFeed(testUser, vibeResult, "공개", true);
            createFeed(testUser, vibeResult2, "비공개", false);
            flushAndClear();

            mockMvc.perform(get("/api/v1/users/{userId}/feeds", testUser.getUserId())
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("타인은 공개 피드만 볼 수 있다")
        void othersSeesPublicOnly() throws Exception {
            createFeed(testUser, vibeResult, "공개", true);
            createFeed(testUser, vibeResult2, "비공개", false);
            flushAndClear();

            mockMvc.perform(get("/api/v1/users/{userId}/feeds", testUser.getUserId())
                            .header("Authorization", bearer(otherAccessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].caption").value("공개"));
        }

        @Test
        @DisplayName("비인증 사용자도 공개 피드 목록을 볼 수 있다")
        void unauthenticated() throws Exception {
            createFeed(testUser, vibeResult, "공개", true);
            createFeed(testUser, vibeResult2, "비공개", false);
            flushAndClear();

            mockMvc.perform(get("/api/v1/users/{userId}/feeds", testUser.getUserId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }
    }

    // ═══════════════════════════════════════════
    // 피드 반응 (토글)
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/feeds/{feedId}/reactions - 피드 반응 토글")
    class ToggleReaction {

        @Test
        @DisplayName("반응을 추가할 수 있다")
        void addReaction() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            flushAndClear();

            mockMvc.perform(post("/api/v1/feeds/{feedId}/reactions", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken))
                            .param("reactionType", "LIKE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reactionType").value("LIKE"))
                    .andExpect(jsonPath("$.data.count").value(1));
        }

        @Test
        @DisplayName("같은 반응을 다시 요청하면 취소된다 (토글)")
        void toggleOff() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            feedReactionRepository.save(FeedReaction.create(feed, otherUser, ReactionType.LIKE));
            flushAndClear();

            mockMvc.perform(post("/api/v1/feeds/{feedId}/reactions", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken))
                            .param("reactionType", "LIKE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reactionType").value("LIKE"))
                    .andExpect(jsonPath("$.data.count").value(0));
        }

        @Test
        @DisplayName("서로 다른 반응 유형은 독립적으로 동작한다")
        void differentReactionTypes() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            feedReactionRepository.save(FeedReaction.create(feed, otherUser, ReactionType.LIKE));
            flushAndClear();

            // LOVE 추가 (LIKE와 독립)
            mockMvc.perform(post("/api/v1/feeds/{feedId}/reactions", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken))
                            .param("reactionType", "LOVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.reactionType").value("LOVE"))
                    .andExpect(jsonPath("$.data.count").value(1));
        }

        @Test
        @DisplayName("미인증 사용자는 반응할 수 없다 (401)")
        void unauthorized() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            flushAndClear();

            mockMvc.perform(post("/api/v1/feeds/{feedId}/reactions", feed.getFeedId())
                            .param("reactionType", "LIKE"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════
    // 댓글
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/feeds/{feedId}/comments - 댓글 작성")
    class CreateComment {

        @Test
        @DisplayName("최상위 댓글을 작성할 수 있다")
        void topLevel() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            flushAndClear();

            var request = new CommentCreateRequest("좋은 피드!", null);

            mockMvc.perform(post("/api/v1/feeds/{feedId}/comments", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("좋은 피드!"))
                    .andExpect(jsonPath("$.data.userId").value(otherUser.getUserId()))
                    .andExpect(jsonPath("$.data.nickname").value("otheruser"))
                    .andExpect(jsonPath("$.data.parentCommentId", nullValue()));
        }

        @Test
        @DisplayName("대댓글을 작성할 수 있다")
        void reply() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment parent = createComment(feed, testUser, null, "원 댓글");
            flushAndClear();

            var request = new CommentCreateRequest("대댓글입니다", parent.getCommentId());

            mockMvc.perform(post("/api/v1/feeds/{feedId}/comments", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("대댓글입니다"))
                    .andExpect(jsonPath("$.data.parentCommentId").value(parent.getCommentId()));
        }

        @Test
        @DisplayName("대댓글에 답글 시 자동으로 루트 댓글에 달린다 (플래트닝)")
        void autoFlatten() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment root = createComment(feed, testUser, null, "루트");
            FeedComment reply = createComment(feed, otherUser, root, "대댓글");
            flushAndClear();

            var request = new CommentCreateRequest("대대댓글", reply.getCommentId());

            mockMvc.perform(post("/api/v1/feeds/{feedId}/comments", feed.getFeedId())
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.parentCommentId").value(root.getCommentId()));
        }

        @Test
        @DisplayName("미인증 사용자는 댓글을 작성할 수 없다 (401)")
        void unauthorized() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            flushAndClear();

            var request = new CommentCreateRequest("댓글", null);

            mockMvc.perform(post("/api/v1/feeds/{feedId}/comments", feed.getFeedId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/feeds/{feedId}/comments - 댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("최상위 댓글 + 대댓글이 nested 형태로 조회된다")
        void withReplies() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment root = createComment(feed, testUser, null, "루트 댓글");
            createComment(feed, otherUser, root, "대댓글1");
            createComment(feed, testUser, root, "대댓글2");
            flushAndClear();

            mockMvc.perform(get("/api/v1/feeds/{feedId}/comments", feed.getFeedId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].content").value("루트 댓글"))
                    .andExpect(jsonPath("$.data.content[0].replies.length()").value(2))
                    .andExpect(jsonPath("$.data.content[0].replies[0].content").value("대댓글1"))
                    .andExpect(jsonPath("$.data.content[0].replies[1].content").value("대댓글2"));
        }

        @Test
        @DisplayName("비인증 사용자도 댓글을 조회할 수 있다")
        void unauthenticated() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            createComment(feed, testUser, null, "댓글");
            flushAndClear();

            mockMvc.perform(get("/api/v1/feeds/{feedId}/comments", feed.getFeedId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }

        @Test
        @DisplayName("인증된 사용자의 좋아요 여부가 표시된다")
        void likeStatus() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, testUser, null, "댓글");
            commentReactionRepository.save(CommentReaction.create(comment, otherUser));
            flushAndClear();

            mockMvc.perform(get("/api/v1/feeds/{feedId}/comments", feed.getFeedId())
                            .header("Authorization", bearer(otherAccessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].likeCount").value(1))
                    .andExpect(jsonPath("$.data.content[0].isLikedByMe").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/comments/{commentId} - 댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("작성자가 댓글을 수정할 수 있다")
        void success() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, testUser, null, "원래 내용");
            flushAndClear();

            var request = new CommentUpdateRequest("수정된 내용");

            mockMvc.perform(put("/api/v1/comments/{commentId}", comment.getCommentId())
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("수정된 내용"));
        }

        @Test
        @DisplayName("작성자가 아닌 사용자는 수정할 수 없다 (403)")
        void notOwner() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, testUser, null, "내용");
            flushAndClear();

            var request = new CommentUpdateRequest("해킹 시도");

            mockMvc.perform(put("/api/v1/comments/{commentId}", comment.getCommentId())
                            .header("Authorization", bearer(otherAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH_002"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/comments/{commentId} - 댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("댓글 작성자가 삭제할 수 있다")
        void byAuthor() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, otherUser, null, "삭제할 댓글");
            Long commentId = comment.getCommentId();
            flushAndClear();

            mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                            .header("Authorization", bearer(otherAccessToken)))
                    .andExpect(status().isOk());

            flushAndClear();
            assertThat(feedCommentRepository.findById(commentId)).isEmpty();
        }

        @Test
        @DisplayName("피드 작성자도 타인의 댓글을 삭제할 수 있다")
        void byFeedOwner() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, otherUser, null, "삭제할 댓글");
            Long commentId = comment.getCommentId();
            flushAndClear();

            mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk());

            flushAndClear();
            assertThat(feedCommentRepository.findById(commentId)).isEmpty();
        }

        @Test
        @DisplayName("권한 없는 사용자는 삭제할 수 없다 (403)")
        void unauthorized() throws Exception {
            // otherUser2를 만들어서 피드 작성자도 댓글 작성자도 아닌 경우 테스트
            User thirdUser = userRepository.save(User.builder()
                    .email("third@example.com")
                    .password("password123")
                    .nickname("thirduser")
                    .name("Third User")
                    .build());
            String thirdToken = jwtTokenProvider.createAccessToken(
                    thirdUser.getUserId(), thirdUser.getEmail());

            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, otherUser, null, "댓글");
            flushAndClear();

            mockMvc.perform(delete("/api/v1/comments/{commentId}", comment.getCommentId())
                            .header("Authorization", bearer(thirdToken)))
                    .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════
    // 댓글 좋아요 (토글)
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/comments/{commentId}/reactions - 댓글 좋아요 토글")
    class ToggleCommentLike {

        @Test
        @DisplayName("댓글 좋아요를 추가할 수 있다")
        void addLike() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, testUser, null, "댓글");
            flushAndClear();

            mockMvc.perform(post("/api/v1/comments/{commentId}/reactions", comment.getCommentId())
                            .header("Authorization", bearer(otherAccessToken)))
                    .andExpect(status().isOk());

            flushAndClear();
            assertThat(commentReactionRepository.countByCommentCommentId(comment.getCommentId()))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("같은 좋아요를 다시 요청하면 취소된다 (토글)")
        void toggleOff() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, testUser, null, "댓글");
            commentReactionRepository.save(CommentReaction.create(comment, otherUser));
            flushAndClear();

            mockMvc.perform(post("/api/v1/comments/{commentId}/reactions", comment.getCommentId())
                            .header("Authorization", bearer(otherAccessToken)))
                    .andExpect(status().isOk());

            flushAndClear();
            assertThat(commentReactionRepository.countByCommentCommentId(comment.getCommentId()))
                    .isEqualTo(0);
        }

        @Test
        @DisplayName("미인증 사용자는 좋아요할 수 없다 (401)")
        void unauthorized() throws Exception {
            Feed feed = createFeed(testUser, vibeResult, "피드", true);
            FeedComment comment = createComment(feed, testUser, null, "댓글");
            flushAndClear();

            mockMvc.perform(post("/api/v1/comments/{commentId}/reactions", comment.getCommentId()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
