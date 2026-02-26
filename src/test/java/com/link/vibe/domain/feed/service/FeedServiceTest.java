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
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @InjectMocks
    private FeedService feedService;

    @Mock private FeedRepository feedRepository;
    @Mock private FeedReactionRepository feedReactionRepository;
    @Mock private FeedCommentRepository feedCommentRepository;
    @Mock private CommentReactionRepository commentReactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private VibeResultRepository vibeResultRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    // ── 테스트 헬퍼 ──

    private User createTestUser(Long userId, String nickname) {
        User user = User.builder()
                .email(nickname + "@test.com")
                .password("password")
                .nickname(nickname)
                .name(nickname)
                .build();
        setField(user, "userId", userId);
        return user;
    }

    private VibeResult createTestVibeResult(Long resultId) {
        VibeResult vr = VibeResult.builder()
                .phrase("테스트 문구")
                .aiAnalysis("분석")
                .aiModelVersion("gpt-4o-mini")
                .build();
        setField(vr, "resultId", resultId);
        setField(vr, "generatedImageUrl", "https://example.com/image.png");
        return vr;
    }

    private Feed createTestFeed(Long feedId, User user, VibeResult vibeResult) {
        Feed feed = Feed.create(user, vibeResult, "테스트 캡션", true);
        setField(feed, "feedId", feedId);
        return feed;
    }

    private FeedComment createTestComment(Long commentId, Feed feed, User user,
                                           FeedComment parent, String content) {
        FeedComment comment = FeedComment.create(feed, user, parent, content);
        setField(comment, "commentId", commentId);
        return comment;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("필드 설정 실패: " + fieldName, e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없음: " + fieldName);
    }

    // ── 피드 CRUD 테스트 ──

    @Nested
    @DisplayName("피드 생성")
    class CreateFeed {

        @Test
        @DisplayName("정상적으로 피드를 생성한다")
        void createFeed_success() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(vibeResultRepository.findById(10L)).willReturn(Optional.of(vr));
            given(feedRepository.existsByUserUserIdAndVibeResultResultId(1L, 10L)).willReturn(false);
            given(feedRepository.save(any(Feed.class))).willAnswer(inv -> {
                Feed saved = inv.getArgument(0);
                setField(saved, "feedId", 100L);
                return saved;
            });
            given(feedReactionRepository.countByFeedIdGroupByReactionType(100L)).willReturn(Collections.emptyList());
            given(feedCommentRepository.countByFeedFeedId(100L)).willReturn(0L);
            given(feedReactionRepository.findByFeedFeedIdAndUserUserId(100L, 1L)).willReturn(Collections.emptyList());

            FeedCreateRequest request = new FeedCreateRequest(10L, "테스트 캡션", true);

            // when
            FeedResponse response = feedService.createFeed(1L, request);

            // then
            assertThat(response.feedId()).isEqualTo(100L);
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.nickname()).isEqualTo("testuser");
            assertThat(response.resultId()).isEqualTo(10L);
            assertThat(response.caption()).isEqualTo("테스트 캡션");
            assertThat(response.isPublic()).isTrue();
            verify(feedRepository).save(any(Feed.class));
        }

        @Test
        @DisplayName("존재하지 않는 Vibe 결과로 생성 시 VIBE_RESULT_NOT_FOUND")
        void createFeed_vibeResultNotFound() {
            // given
            User user = createTestUser(1L, "testuser");
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(vibeResultRepository.findById(999L)).willReturn(Optional.empty());

            FeedCreateRequest request = new FeedCreateRequest(999L, "캡션", true);

            // when & then
            assertThatThrownBy(() -> feedService.createFeed(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.VIBE_RESULT_NOT_FOUND);
        }

        @Test
        @DisplayName("동일한 Vibe 결과로 중복 생성 시 FEED_ALREADY_EXISTS")
        void createFeed_duplicate() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(vibeResultRepository.findById(10L)).willReturn(Optional.of(vr));
            given(feedRepository.existsByUserUserIdAndVibeResultResultId(1L, 10L)).willReturn(true);

            FeedCreateRequest request = new FeedCreateRequest(10L, "캡션", true);

            // when & then
            assertThatThrownBy(() -> feedService.createFeed(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FEED_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("피드 수정")
    class UpdateFeed {

        @Test
        @DisplayName("피드 소유자가 캡션과 공개 여부를 수정한다")
        void updateFeed_success() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));
            given(feedReactionRepository.countByFeedIdGroupByReactionType(100L)).willReturn(Collections.emptyList());
            given(feedCommentRepository.countByFeedFeedId(100L)).willReturn(0L);
            given(feedReactionRepository.findByFeedFeedIdAndUserUserId(100L, 1L)).willReturn(Collections.emptyList());

            FeedUpdateRequest request = new FeedUpdateRequest("수정된 캡션", false);

            // when
            FeedResponse response = feedService.updateFeed(1L, 100L, request);

            // then
            assertThat(response.caption()).isEqualTo("수정된 캡션");
            assertThat(response.isPublic()).isFalse();
        }

        @Test
        @DisplayName("소유자가 아닌 사용자가 수정 시 ACCESS_DENIED")
        void updateFeed_accessDenied() {
            // given
            User owner = createTestUser(1L, "owner");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, owner, vr);

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));

            FeedUpdateRequest request = new FeedUpdateRequest("수정", null);

            // when & then
            assertThatThrownBy(() -> feedService.updateFeed(2L, 100L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("피드 삭제")
    class DeleteFeed {

        @Test
        @DisplayName("피드 소유자가 소프트 삭제한다")
        void deleteFeed_success() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));

            // when
            feedService.deleteFeed(1L, 100L);

            // then
            assertThat(feed.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("소유자가 아닌 사용자가 삭제 시 ACCESS_DENIED")
        void deleteFeed_accessDenied() {
            // given
            User owner = createTestUser(1L, "owner");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, owner, vr);

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));

            // when & then
            assertThatThrownBy(() -> feedService.deleteFeed(2L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("피드 타임라인 조회")
    class GetFeedTimeline {

        @Test
        @DisplayName("공개 피드를 커서 기반으로 조회한다")
        void getFeedTimeline_success() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed1 = createTestFeed(102L, user, vr);
            Feed feed2 = createTestFeed(101L, user, vr);

            given(feedRepository.findPublicFeeds(isNull(), any(Pageable.class)))
                    .willReturn(List.of(feed1, feed2));
            given(feedReactionRepository.countByFeedIdGroupByReactionType(anyLong())).willReturn(Collections.emptyList());
            given(feedCommentRepository.countByFeedFeedId(anyLong())).willReturn(0L);
            given(feedReactionRepository.findByFeedFeedIdAndUserUserId(anyLong(), eq(1L))).willReturn(Collections.emptyList());

            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            // when
            PageResponse<FeedResponse> response = feedService.getFeedTimeline(1L, pageRequest);

            // then
            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).feedId()).isEqualTo(102L);
        }
    }

    // ── 피드 반응 토글 테스트 ──

    @Nested
    @DisplayName("피드 반응 토글")
    class ToggleReaction {

        @Test
        @DisplayName("반응이 없으면 추가한다")
        void toggleReaction_add() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedReactionRepository.findByFeedFeedIdAndUserUserIdAndReactionType(100L, 1L, ReactionType.LIKE))
                    .willReturn(Optional.empty());
            given(feedReactionRepository.save(any(FeedReaction.class))).willAnswer(inv -> inv.getArgument(0));
            given(feedReactionRepository.countByFeedIdGroupByReactionType(100L))
                    .willReturn(List.<Object[]>of(new Object[]{ReactionType.LIKE, 1L}));

            // when
            ReactionSummary result = feedService.toggleReaction(1L, 100L, ReactionType.LIKE);

            // then
            assertThat(result.reactionType()).isEqualTo("LIKE");
            assertThat(result.count()).isEqualTo(1L);
            verify(feedReactionRepository).save(any(FeedReaction.class));
            verify(eventPublisher).publishEvent((Object) any());
        }

        @Test
        @DisplayName("반응이 이미 있으면 삭제한다")
        void toggleReaction_remove() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);
            FeedReaction existing = FeedReaction.create(feed, user, ReactionType.LIKE);

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedReactionRepository.findByFeedFeedIdAndUserUserIdAndReactionType(100L, 1L, ReactionType.LIKE))
                    .willReturn(Optional.of(existing));
            given(feedReactionRepository.countByFeedIdGroupByReactionType(100L))
                    .willReturn(Collections.emptyList());

            // when
            ReactionSummary result = feedService.toggleReaction(1L, 100L, ReactionType.LIKE);

            // then
            assertThat(result.reactionType()).isEqualTo("LIKE");
            assertThat(result.count()).isEqualTo(0L);
            verify(feedReactionRepository).delete(existing);
            verify(eventPublisher, never()).publishEvent((Object) any());
        }
    }

    // ── 댓글 테스트 ──

    @Nested
    @DisplayName("댓글 작성")
    class CreateComment {

        @Test
        @DisplayName("최상위 댓글을 작성한다")
        void createComment_topLevel() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedCommentRepository.save(any(FeedComment.class))).willAnswer(inv -> {
                FeedComment saved = inv.getArgument(0);
                setField(saved, "commentId", 200L);
                return saved;
            });
            given(commentReactionRepository.countByCommentCommentId(200L)).willReturn(0L);
            given(commentReactionRepository.existsByCommentCommentIdAndUserUserId(200L, 1L)).willReturn(false);

            CommentCreateRequest request = new CommentCreateRequest("댓글 내용", null);

            // when
            CommentResponse response = feedService.createComment(1L, 100L, request);

            // then
            assertThat(response.commentId()).isEqualTo(200L);
            assertThat(response.content()).isEqualTo("댓글 내용");
            assertThat(response.parentCommentId()).isNull();
            verify(eventPublisher).publishEvent((Object) any());
        }

        @Test
        @DisplayName("대댓글을 작성한다")
        void createComment_reply() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);
            FeedComment parent = createTestComment(200L, feed, user, null, "부모 댓글");

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedCommentRepository.findById(200L)).willReturn(Optional.of(parent));
            given(feedCommentRepository.save(any(FeedComment.class))).willAnswer(inv -> {
                FeedComment saved = inv.getArgument(0);
                setField(saved, "commentId", 201L);
                return saved;
            });
            given(commentReactionRepository.countByCommentCommentId(201L)).willReturn(0L);
            given(commentReactionRepository.existsByCommentCommentIdAndUserUserId(201L, 1L)).willReturn(false);

            CommentCreateRequest request = new CommentCreateRequest("대댓글", 200L);

            // when
            CommentResponse response = feedService.createComment(1L, 100L, request);

            // then
            assertThat(response.commentId()).isEqualTo(201L);
            assertThat(response.parentCommentId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("대댓글에 답글 시 자동 플래트닝으로 루트 부모에 배치된다")
        void createComment_autoFlattening() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);
            FeedComment rootComment = createTestComment(200L, feed, user, null, "루트 댓글");
            FeedComment replyComment = createTestComment(201L, feed, user, rootComment, "대댓글");

            given(feedRepository.findById(100L)).willReturn(Optional.of(feed));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(feedCommentRepository.findById(201L)).willReturn(Optional.of(replyComment));
            given(feedCommentRepository.save(any(FeedComment.class))).willAnswer(inv -> {
                FeedComment saved = inv.getArgument(0);
                setField(saved, "commentId", 202L);
                return saved;
            });
            given(commentReactionRepository.countByCommentCommentId(202L)).willReturn(0L);
            given(commentReactionRepository.existsByCommentCommentIdAndUserUserId(202L, 1L)).willReturn(false);

            // 대댓글(201)에 답글 → parentCommentId = 201
            CommentCreateRequest request = new CommentCreateRequest("@user 대댓글에 답글", 201L);

            // when
            CommentResponse response = feedService.createComment(1L, 100L, request);

            // then — 자동 플래트닝: parentCommentId가 루트(200)로 변환
            assertThat(response.parentCommentId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("댓글 작성자가 내용을 수정한다")
        void updateComment_success() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);
            FeedComment comment = createTestComment(200L, feed, user, null, "원래 내용");

            given(feedCommentRepository.findById(200L)).willReturn(Optional.of(comment));
            given(commentReactionRepository.countByCommentCommentId(200L)).willReturn(0L);
            given(commentReactionRepository.existsByCommentCommentIdAndUserUserId(200L, 1L)).willReturn(false);

            CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");

            // when
            CommentResponse response = feedService.updateComment(1L, 200L, request);

            // then
            assertThat(response.content()).isEqualTo("수정된 내용");
        }

        @Test
        @DisplayName("작성자가 아닌 사용자가 수정 시 ACCESS_DENIED")
        void updateComment_accessDenied() {
            // given
            User owner = createTestUser(1L, "owner");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, owner, vr);
            FeedComment comment = createTestComment(200L, feed, owner, null, "내용");

            given(feedCommentRepository.findById(200L)).willReturn(Optional.of(comment));

            CommentUpdateRequest request = new CommentUpdateRequest("수정");

            // when & then
            assertThatThrownBy(() -> feedService.updateComment(2L, 200L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("댓글 작성자가 삭제한다")
        void deleteComment_byAuthor() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);
            FeedComment comment = createTestComment(200L, feed, user, null, "삭제할 댓글");

            given(feedCommentRepository.findById(200L)).willReturn(Optional.of(comment));

            // when
            feedService.deleteComment(1L, 200L);

            // then
            assertThat(comment.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("피드 소유자가 타인의 댓글을 삭제한다")
        void deleteComment_byFeedOwner() {
            // given
            User feedOwner = createTestUser(1L, "feedOwner");
            User commenter = createTestUser(2L, "commenter");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, feedOwner, vr);
            FeedComment comment = createTestComment(200L, feed, commenter, null, "타인의 댓글");

            given(feedCommentRepository.findById(200L)).willReturn(Optional.of(comment));

            // when
            feedService.deleteComment(1L, 200L);

            // then
            assertThat(comment.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("피드 소유자도 댓글 작성자도 아닌 사용자가 삭제 시 ACCESS_DENIED")
        void deleteComment_accessDenied() {
            // given
            User feedOwner = createTestUser(1L, "feedOwner");
            User commenter = createTestUser(2L, "commenter");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, feedOwner, vr);
            FeedComment comment = createTestComment(200L, feed, commenter, null, "댓글");

            given(feedCommentRepository.findById(200L)).willReturn(Optional.of(comment));

            // when & then
            assertThatThrownBy(() -> feedService.deleteComment(3L, 200L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("댓글 좋아요 토글")
    class ToggleCommentLike {

        @Test
        @DisplayName("좋아요가 없으면 추가한다")
        void toggleCommentLike_add() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);
            FeedComment comment = createTestComment(200L, feed, user, null, "댓글");

            given(feedCommentRepository.findById(200L)).willReturn(Optional.of(comment));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(commentReactionRepository.findByCommentCommentIdAndUserUserId(200L, 1L))
                    .willReturn(Optional.empty());

            // when
            feedService.toggleCommentLike(1L, 200L);

            // then
            verify(commentReactionRepository).save(any(CommentReaction.class));
        }

        @Test
        @DisplayName("좋아요가 있으면 삭제한다")
        void toggleCommentLike_remove() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);
            FeedComment comment = createTestComment(200L, feed, user, null, "댓글");
            CommentReaction existing = CommentReaction.create(comment, user);

            given(feedCommentRepository.findById(200L)).willReturn(Optional.of(comment));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(commentReactionRepository.findByCommentCommentIdAndUserUserId(200L, 1L))
                    .willReturn(Optional.of(existing));

            // when
            feedService.toggleCommentLike(1L, 200L);

            // then
            verify(commentReactionRepository).delete(existing);
        }
    }

    // ── 댓글 목록 조회 테스트 ──

    @Nested
    @DisplayName("댓글 목록 조회")
    class GetComments {

        @Test
        @DisplayName("최상위 댓글과 대댓글을 nested로 조회한다")
        void getComments_withReplies() {
            // given
            User user = createTestUser(1L, "testuser");
            VibeResult vr = createTestVibeResult(10L);
            Feed feed = createTestFeed(100L, user, vr);
            FeedComment parent = createTestComment(200L, feed, user, null, "부모 댓글");
            FeedComment reply = createTestComment(201L, feed, user, parent, "대댓글");

            given(feedCommentRepository.findTopLevelComments(eq(100L), isNull(), any(Pageable.class)))
                    .willReturn(List.of(parent));
            given(feedCommentRepository.findByParentCommentCommentIdOrderByCommentIdAsc(200L))
                    .willReturn(List.of(reply));
            given(commentReactionRepository.countByCommentCommentId(anyLong())).willReturn(0L);
            given(commentReactionRepository.existsByCommentCommentIdAndUserUserId(anyLong(), eq(1L))).willReturn(false);

            CursorPageRequest pageRequest = new CursorPageRequest();
            pageRequest.setSize(20);

            // when
            PageResponse<CommentResponse> response = feedService.getComments(100L, 1L, pageRequest);

            // then
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).commentId()).isEqualTo(200L);
            assertThat(response.content().get(0).replies()).hasSize(1);
            assertThat(response.content().get(0).replies().get(0).commentId()).isEqualTo(201L);
        }
    }

    // ── 피드 조회 에러 케이스 ──

    @Nested
    @DisplayName("에러 케이스")
    class ErrorCases {

        @Test
        @DisplayName("존재하지 않는 피드 조회 시 FEED_NOT_FOUND")
        void getFeedDetail_notFound() {
            given(feedRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> feedService.getFeedDetail(999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FEED_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 댓글 조회 시 COMMENT_NOT_FOUND")
        void findComment_notFound() {
            given(feedCommentRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> feedService.deleteComment(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
        }
    }
}
