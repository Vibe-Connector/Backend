package com.link.vibe.domain.explore.service;

import com.link.vibe.domain.explore.dto.ExplorePeriod;
import com.link.vibe.domain.explore.dto.ExploreVibeResponse;
import com.link.vibe.domain.feed.repository.FeedRepository;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExploreServiceTest {

    @InjectMocks
    private ExploreService exploreService;

    @Mock
    private FeedRepository feedRepository;

    // ── 테스트 헬퍼 ──

    private CursorPageRequest createPageRequest(String cursor, int size) {
        CursorPageRequest request = new CursorPageRequest();
        request.setCursor(cursor);
        request.setSize(size);
        return request;
    }

    private Object[] createRow(Long feedId, String caption, Integer viewCount,
                                Long authorId, String nickname, String profileImageUrl,
                                String imageUrl, Long reactionCnt, Long commentCnt,
                                Long score) {
        return new Object[]{
            feedId, caption, viewCount,
            Timestamp.valueOf(LocalDateTime.now()),
            authorId, nickname, profileImageUrl,
            imageUrl,
            reactionCnt, commentCnt, score
        };
    }

    // ═══════════════════════════════════════════
    // getPopularVibes
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("getPopularVibes")
    class GetPopularVibes {

        @Test
        @DisplayName("첫 페이지를 정상 조회한다 (커서 없음)")
        void firstPage() {
            // given
            CursorPageRequest request = createPageRequest(null, 20);
            List<Object[]> rows = List.of(
                createRow(1L, "피드1", 100, 10L, "user1", null, "img1.png", 5L, 3L, 180L),
                createRow(2L, "피드2", 50, 11L, "user2", null, "img2.png", 2L, 1L, 80L)
            );

            given(feedRepository.findPopularFeeds(
                any(LocalDateTime.class), eq(Long.MAX_VALUE), eq(Long.MAX_VALUE), any(Pageable.class)))
                .willReturn(rows);

            // when
            PageResponse<ExploreVibeResponse> result =
                exploreService.getPopularVibes(ExplorePeriod.WEEK, request, 1L);

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.hasNext()).isFalse();

            ExploreVibeResponse first = result.content().get(0);
            assertThat(first.feedId()).isEqualTo(1L);
            assertThat(first.caption()).isEqualTo("피드1");
            assertThat(first.viewCount()).isEqualTo(100);
            assertThat(first.authorId()).isEqualTo(10L);
            assertThat(first.authorNickname()).isEqualTo("user1");
            assertThat(first.generatedImageUrl()).isEqualTo("img1.png");
            assertThat(first.reactionCount()).isEqualTo(5L);
            assertThat(first.commentCount()).isEqualTo(3L);
            assertThat(first.popularityScore()).isEqualTo(180L);
        }

        @Test
        @DisplayName("커서가 있는 경우 파싱하여 전달한다")
        void withCursor() {
            // given
            CursorPageRequest request = createPageRequest("150_42", 20);

            given(feedRepository.findPopularFeeds(
                any(LocalDateTime.class), eq(150L), eq(42L), any(Pageable.class)))
                .willReturn(Collections.emptyList());

            // when
            PageResponse<ExploreVibeResponse> result =
                exploreService.getPopularVibes(ExplorePeriod.MONTH, request, 1L);

            // then
            verify(feedRepository).findPopularFeeds(
                any(LocalDateTime.class), eq(150L), eq(42L), any(Pageable.class));
            assertThat(result.content()).isEmpty();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("결과가 비어있으면 빈 응답을 반환한다")
        void emptyResult() {
            // given
            CursorPageRequest request = createPageRequest(null, 20);

            given(feedRepository.findPopularFeeds(
                any(LocalDateTime.class), any(), any(), any(Pageable.class)))
                .willReturn(Collections.emptyList());

            // when
            PageResponse<ExploreVibeResponse> result =
                exploreService.getPopularVibes(ExplorePeriod.DAY, request, 1L);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
        }

        @Test
        @DisplayName("hasNext가 true이면 nextCursor가 설정된다")
        void hasNextWithCursor() {
            // given
            CursorPageRequest request = createPageRequest(null, 2);
            // getFetchSize() = 2 + 1 = 3 → 3개 반환하면 hasNext=true
            List<Object[]> rows = List.of(
                createRow(10L, "A", 50, 1L, "u1", null, "i1", 3L, 2L, 500L),
                createRow(20L, "B", 30, 2L, "u2", null, "i2", 1L, 0L, 200L),
                createRow(30L, "C", 10, 3L, "u3", null, "i3", 0L, 0L, 10L)
            );

            given(feedRepository.findPopularFeeds(
                any(LocalDateTime.class), any(), any(), any(Pageable.class)))
                .willReturn(rows);

            // when
            PageResponse<ExploreVibeResponse> result =
                exploreService.getPopularVibes(ExplorePeriod.WEEK, request, 1L);

            // then
            assertThat(result.content()).hasSize(2);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo("200_20");
        }

        @Test
        @DisplayName("각 기간 필터(DAY, WEEK, MONTH)가 올바르게 전달된다")
        void periodFilters() {
            // given
            CursorPageRequest request = createPageRequest(null, 20);

            given(feedRepository.findPopularFeeds(
                any(LocalDateTime.class), any(), any(), any(Pageable.class)))
                .willReturn(Collections.emptyList());

            // when & then - DAY
            exploreService.getPopularVibes(ExplorePeriod.DAY, request, 1L);
            // when & then - WEEK
            exploreService.getPopularVibes(ExplorePeriod.WEEK, request, 1L);
            // when & then - MONTH
            exploreService.getPopularVibes(ExplorePeriod.MONTH, request, 1L);

            verify(feedRepository, org.mockito.Mockito.times(3)).findPopularFeeds(
                any(LocalDateTime.class), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("잘못된 커서 형식이면 예외가 발생한다")
        void invalidCursorFormat() {
            // given
            CursorPageRequest request = createPageRequest("invalid", 20);

            // when & then
            assertThatThrownBy(() ->
                exploreService.getPopularVibes(ExplorePeriod.WEEK, request, 1L))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("null 필드가 있는 행도 정상 매핑된다")
        void nullFieldsInRow() {
            // given
            CursorPageRequest request = createPageRequest(null, 20);
            List<Object[]> rows = new java.util.ArrayList<>();
            rows.add(createRow(1L, null, 0, 10L, "user1", null, null, 0L, 0L, 0L));

            given(feedRepository.findPopularFeeds(
                any(LocalDateTime.class), any(), any(), any(Pageable.class)))
                .willReturn(rows);

            // when
            PageResponse<ExploreVibeResponse> result =
                exploreService.getPopularVibes(ExplorePeriod.WEEK, request, 1L);

            // then
            assertThat(result.content()).hasSize(1);
            ExploreVibeResponse item = result.content().get(0);
            assertThat(item.caption()).isNull();
            assertThat(item.generatedImageUrl()).isNull();
            assertThat(item.authorProfileImageUrl()).isNull();
            assertThat(item.popularityScore()).isEqualTo(0L);
        }
    }
}
