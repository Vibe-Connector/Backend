package com.link.vibe.domain.feed.benchmark;

import com.link.vibe.config.TestMailConfig;
import com.link.vibe.config.TestRedisConfig;
import com.link.vibe.config.TestS3Config;
import com.link.vibe.domain.feed.dto.FeedResponse;
import com.link.vibe.domain.feed.dto.ReactionSummary;
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
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestRedisConfig.class, TestS3Config.class, TestMailConfig.class})
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FeedQueryBenchmarkTest {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Autowired UserRepository userRepository;
    @Autowired VibeSessionRepository vibeSessionRepository;
    @Autowired VibeResultRepository vibeResultRepository;
    @Autowired FeedRepository feedRepository;
    @Autowired FeedReactionRepository feedReactionRepository;
    @Autowired FeedCommentRepository feedCommentRepository;

    private static final int FEED_COUNT = 20;
    private static final int REACTIONS_PER_FEED = 3;
    private static final int COMMENTS_PER_FEED = 2;

    private List<User> users;
    private List<Feed> feeds;
    private String accessToken;
    private Long currentUserId;

    // 전략별 결과를 저장하여 마지막에 비교 테이블 출력
    private static final List<BenchmarkResult> results = Collections.synchronizedList(new ArrayList<>());

    record BenchmarkResult(String strategy, long queryCount, long entityLoadCount,
                           long maxQueryTimeMs, long apiResponseTimeMs) {}

    @BeforeEach
    void seedData() {
        // 1. 유저 5명 생성
        users = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> userRepository.save(User.builder()
                        .email("bench" + i + "@test.com")
                        .password("password123")
                        .nickname("benchuser" + i)
                        .name("Bench User " + i)
                        .build()))
                .toList();

        // 2. 피드 20개 생성 (공개)
        feeds = new ArrayList<>();
        for (int i = 0; i < FEED_COUNT; i++) {
            User owner = users.get(i % 5);
            VibeSession session = vibeSessionRepository.save(
                    VibeSession.builder().userId(owner.getUserId()).build());
            VibeResult result = vibeResultRepository.save(VibeResult.builder()
                    .vibeSession(session)
                    .phrase("Phrase " + i)
                    .aiAnalysis("Analysis " + i)
                    .aiModelVersion("test")
                    .processingTimeMs(100)
                    .build());
            feeds.add(feedRepository.save(
                    Feed.create(owner, result, "Caption " + i, true)));
        }

        // 3. 반응 60개 (피드당 3개, 다른 유저로)
        ReactionType[] types = ReactionType.values();
        for (int i = 0; i < FEED_COUNT; i++) {
            for (int j = 0; j < REACTIONS_PER_FEED; j++) {
                User reactor = users.get((i + j + 1) % 5);
                feedReactionRepository.save(
                        FeedReaction.create(feeds.get(i), reactor, types[j % types.length]));
            }
        }

        // 4. 댓글 40개 (피드당 2개)
        for (int i = 0; i < FEED_COUNT; i++) {
            for (int j = 0; j < COMMENTS_PER_FEED; j++) {
                User commenter = users.get((i + j + 2) % 5);
                feedCommentRepository.save(
                        FeedComment.create(feeds.get(i), commenter, null,
                                "Comment " + j + " on feed " + i));
            }
        }

        // 5. 인증 토큰 (user1 기준)
        currentUserId = users.get(0).getUserId();
        accessToken = jwtTokenProvider.createAccessToken(
                currentUserId, users.get(0).getEmail());

        // 6. 영속성 컨텍스트 초기화 (cold start)
        em.flush();
        em.clear();
    }

    // ── 측정 헬퍼 ──

    private Statistics resetStats() {
        em.flush();
        em.clear();
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        return stats;
    }

    private BenchmarkResult recordResult(String strategyName, Statistics stats, long startNano) {
        long apiTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
        long queryCount = stats.getPrepareStatementCount();
        long entityLoadCount = stats.getEntityLoadCount();
        long maxQueryTimeMs = stats.getQueryExecutionMaxTime();

        BenchmarkResult result = new BenchmarkResult(
                strategyName, queryCount, entityLoadCount, maxQueryTimeMs, apiTimeMs);
        results.add(result);

        System.out.printf("""
                ┌─────────────────────────────────────────────────┐
                │ Strategy: %-37s │
                │ DB Query Count:      %-26d │
                │ Entity Load Count:   %-26d │
                │ Max Query Time:      %-22d ms │
                │ API Response Time:   %-22d ms │
                └─────────────────────────────────────────────────┘
                """,
                strategyName, queryCount, entityLoadCount, maxQueryTimeMs, apiTimeMs);

        return result;
    }

    // ═══════════════════════════════════════════
    // Strategy A: N+1 Baseline
    // ═══════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("[Strategy A] N+1 Baseline — 피드당 개별 쿼리")
    void strategyA_n1Baseline() {
        Statistics stats = resetStats();
        long startNano = System.nanoTime();

        // 1. 메인 리스트 쿼리
        List<Feed> feedList = feedRepository.findPublicFeeds(null, PageRequest.of(0, 21));
        List<Feed> page = feedList.subList(0, Math.min(feedList.size(), FEED_COUNT));

        // 2. 피드당 개별 서브쿼리 (N+1 패턴 재현)
        List<FeedResponse> responses = new ArrayList<>();
        for (Feed feed : page) {
            // LAZY 로딩 트리거
            Long userId = feed.getUser().getUserId();
            String nickname = feed.getUser().getNickname();
            String profileImg = feed.getUser().getProfileImageUrl();
            Long resultId = feed.getVibeResult().getResultId();
            String imageUrl = feed.getVibeResult().getGeneratedImageUrl();
            String phrase = feed.getVibeResult().getPhrase();

            // 개별 쿼리
            List<ReactionSummary> reactions = feedReactionRepository
                    .countByFeedIdGroupByReactionType(feed.getFeedId()).stream()
                    .map(row -> new ReactionSummary(
                            ((ReactionType) row[0]).getValue(), (Long) row[1]))
                    .toList();
            long commentCount = feedCommentRepository.countByFeedFeedId(feed.getFeedId());
            List<String> myReactions = feedReactionRepository
                    .findByFeedFeedIdAndUserUserId(feed.getFeedId(), currentUserId)
                    .map(r -> List.of(r.getReactionType().getValue()))
                    .orElse(Collections.emptyList());

            responses.add(new FeedResponse(feed.getFeedId(), userId, nickname, profileImg,
                    resultId, imageUrl, phrase, feed.getCaption(), feed.getIsPublic(),
                    feed.getViewCount(), reactions, commentCount, myReactions,
                    feed.getCreatedAt(), feed.getUpdatedAt()));
        }

        BenchmarkResult result = recordResult("A: N+1 Baseline", stats, startNano);
        assertThat(responses).hasSize(FEED_COUNT);
        assertThat(result.queryCount()).isGreaterThan(50); // N+1 확인
    }

    // ═══════════════════════════════════════════
    // Strategy B: Batch IN Query
    // ═══════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("[Strategy B] Batch IN Query — WHERE feedId IN 배치")
    void strategyB_batchInQuery() {
        Statistics stats = resetStats();
        long startNano = System.nanoTime();

        // 1. 메인 리스트 쿼리
        List<Feed> feedList = feedRepository.findPublicFeeds(null, PageRequest.of(0, 21));
        List<Feed> page = feedList.subList(0, Math.min(feedList.size(), FEED_COUNT));
        List<Long> feedIds = page.stream().map(Feed::getFeedId).toList();

        // 2. 배치 쿼리 3개
        Map<Long, List<ReactionSummary>> reactionsMap = new HashMap<>();
        feedReactionRepository.countByFeedIdsGroupByReactionType(feedIds)
                .forEach(row -> {
                    Long feedId = (Long) row[0];
                    String type = ((ReactionType) row[1]).getValue();
                    Long count = (Long) row[2];
                    reactionsMap.computeIfAbsent(feedId, k -> new ArrayList<>())
                            .add(new ReactionSummary(type, count));
                });

        Map<Long, Long> commentCountMap = new HashMap<>();
        feedCommentRepository.countByFeedFeedIdIn(feedIds)
                .forEach(row -> commentCountMap.put((Long) row[0], (Long) row[1]));

        Map<Long, List<String>> myReactionsMap = new HashMap<>();
        feedReactionRepository.findByFeedFeedIdInAndUserUserId(feedIds, currentUserId)
                .forEach(fr -> myReactionsMap.put(
                        fr.getFeed().getFeedId(),
                        List.of(fr.getReactionType().getValue())));

        // 3. 응답 조립 (User, VibeResult 접근 시 LAZY 로딩 발생)
        List<FeedResponse> responses = page.stream().map(feed -> {
            Long fid = feed.getFeedId();
            return new FeedResponse(fid,
                    feed.getUser().getUserId(), feed.getUser().getNickname(),
                    feed.getUser().getProfileImageUrl(),
                    feed.getVibeResult().getResultId(),
                    feed.getVibeResult().getGeneratedImageUrl(),
                    feed.getVibeResult().getPhrase(),
                    feed.getCaption(), feed.getIsPublic(), feed.getViewCount(),
                    reactionsMap.getOrDefault(fid, Collections.emptyList()),
                    commentCountMap.getOrDefault(fid, 0L),
                    myReactionsMap.getOrDefault(fid, Collections.emptyList()),
                    feed.getCreatedAt(), feed.getUpdatedAt());
        }).toList();

        BenchmarkResult result = recordResult("B: Batch IN Query", stats, startNano);
        assertThat(responses).hasSize(FEED_COUNT);
        assertThat(result.queryCount()).isLessThan(60); // A보다 줄어야 함
    }

    // ═══════════════════════════════════════════
    // Strategy D: @EntityGraph (User, VibeResult 즉시 로딩)
    // ═══════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("[Strategy D] @EntityGraph — User/VibeResult JOIN FETCH")
    void strategyD_entityGraph() {
        Statistics stats = resetStats();
        long startNano = System.nanoTime();

        // 1. @EntityGraph로 User, VibeResult 즉시 로딩
        List<Feed> feedList = feedRepository.findPublicFeedsWithFetch(null, PageRequest.of(0, 21));
        List<Feed> page = feedList.subList(0, Math.min(feedList.size(), FEED_COUNT));

        // 2. 피드당 개별 서브쿼리 (반응/댓글은 여전히 N+1)
        List<FeedResponse> responses = new ArrayList<>();
        for (Feed feed : page) {
            List<ReactionSummary> reactions = feedReactionRepository
                    .countByFeedIdGroupByReactionType(feed.getFeedId()).stream()
                    .map(row -> new ReactionSummary(
                            ((ReactionType) row[0]).getValue(), (Long) row[1]))
                    .toList();
            long commentCount = feedCommentRepository.countByFeedFeedId(feed.getFeedId());
            List<String> myReactions = feedReactionRepository
                    .findByFeedFeedIdAndUserUserId(feed.getFeedId(), currentUserId)
                    .map(r -> List.of(r.getReactionType().getValue()))
                    .orElse(Collections.emptyList());

            responses.add(new FeedResponse(feed.getFeedId(),
                    feed.getUser().getUserId(), feed.getUser().getNickname(),
                    feed.getUser().getProfileImageUrl(),
                    feed.getVibeResult().getResultId(),
                    feed.getVibeResult().getGeneratedImageUrl(),
                    feed.getVibeResult().getPhrase(),
                    feed.getCaption(), feed.getIsPublic(), feed.getViewCount(),
                    reactions, commentCount, myReactions,
                    feed.getCreatedAt(), feed.getUpdatedAt()));
        }

        BenchmarkResult result = recordResult("D: @EntityGraph", stats, startNano);
        assertThat(responses).hasSize(FEED_COUNT);
        assertThat(result.queryCount()).isLessThan(101); // A보다 적어야 함 (User/VR LAZY 제거, 반응/댓글 N+1 잔존)
    }

    // ═══════════════════════════════════════════
    // Strategy B+D: Batch + @EntityGraph (최적 JPA 조합)
    // ═══════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("[Strategy B+D] Batch + @EntityGraph — 최적 JPA 조합")
    void strategyBD_batchPlusEntityGraph() {
        Statistics stats = resetStats();
        long startNano = System.nanoTime();

        // 1. @EntityGraph로 User, VibeResult 즉시 로딩
        List<Feed> feedList = feedRepository.findPublicFeedsWithFetch(null, PageRequest.of(0, 21));
        List<Feed> page = feedList.subList(0, Math.min(feedList.size(), FEED_COUNT));
        List<Long> feedIds = page.stream().map(Feed::getFeedId).toList();

        // 2. 배치 쿼리 3개
        Map<Long, List<ReactionSummary>> reactionsMap = new HashMap<>();
        feedReactionRepository.countByFeedIdsGroupByReactionType(feedIds)
                .forEach(row -> {
                    Long feedId = (Long) row[0];
                    String type = ((ReactionType) row[1]).getValue();
                    Long count = (Long) row[2];
                    reactionsMap.computeIfAbsent(feedId, k -> new ArrayList<>())
                            .add(new ReactionSummary(type, count));
                });

        Map<Long, Long> commentCountMap = new HashMap<>();
        feedCommentRepository.countByFeedFeedIdIn(feedIds)
                .forEach(row -> commentCountMap.put((Long) row[0], (Long) row[1]));

        Map<Long, List<String>> myReactionsMap = new HashMap<>();
        feedReactionRepository.findByFeedFeedIdInAndUserUserId(feedIds, currentUserId)
                .forEach(fr -> myReactionsMap.put(
                        fr.getFeed().getFeedId(),
                        List.of(fr.getReactionType().getValue())));

        // 3. 응답 조립 (User/VibeResult는 이미 로딩됨 — 추가 쿼리 없음)
        List<FeedResponse> responses = page.stream().map(feed -> {
            Long fid = feed.getFeedId();
            return new FeedResponse(fid,
                    feed.getUser().getUserId(), feed.getUser().getNickname(),
                    feed.getUser().getProfileImageUrl(),
                    feed.getVibeResult().getResultId(),
                    feed.getVibeResult().getGeneratedImageUrl(),
                    feed.getVibeResult().getPhrase(),
                    feed.getCaption(), feed.getIsPublic(), feed.getViewCount(),
                    reactionsMap.getOrDefault(fid, Collections.emptyList()),
                    commentCountMap.getOrDefault(fid, 0L),
                    myReactionsMap.getOrDefault(fid, Collections.emptyList()),
                    feed.getCreatedAt(), feed.getUpdatedAt());
        }).toList();

        BenchmarkResult result = recordResult("B+D: Batch + EntityGraph", stats, startNano);
        assertThat(responses).hasSize(FEED_COUNT);
        assertThat(result.queryCount()).isLessThanOrEqualTo(10); // 목표: ~4 쿼리
    }

    // ═══════════════════════════════════════════
    // Strategy C: Native JOIN (단일 쿼리)
    // ═══════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("[Strategy C] Native JOIN — 단일 쿼리 집계")
    void strategyC_nativeJoin() {
        Statistics stats = resetStats();
        long startNano = System.nanoTime();

        // 단일 네이티브 쿼리로 모든 데이터 조회
        List<Object[]> rows = feedRepository.findPublicFeedsWithJoin(
                null, currentUserId, PageRequest.of(0, 21));

        List<FeedResponse> responses = rows.stream().map(row -> {
            Long feedId = ((Number) row[0]).longValue();
            String caption = (String) row[1];
            Boolean isPublic = (Boolean) row[2];
            Integer viewCount = ((Number) row[3]).intValue();
            LocalDateTime createdAt = row[4] instanceof LocalDateTime ldt ? ldt
                    : ((java.sql.Timestamp) row[4]).toLocalDateTime();
            LocalDateTime updatedAt = row[5] != null
                    ? (row[5] instanceof LocalDateTime ldt2 ? ldt2
                    : ((java.sql.Timestamp) row[5]).toLocalDateTime())
                    : null;
            Long userId = ((Number) row[6]).longValue();
            String nickname = (String) row[7];
            String profileImageUrl = (String) row[8];
            Long resultId = ((Number) row[9]).longValue();
            String generatedImageUrl = (String) row[10];
            String phrase = (String) row[11];
            long commentCount = ((Number) row[12]).longValue();

            // 반응 카운트를 ReactionSummary 리스트로 변환
            List<ReactionSummary> reactions = new ArrayList<>();
            long likeCount = ((Number) row[13]).longValue();
            long dislikeCount = ((Number) row[14]).longValue();
            long wowCount = ((Number) row[15]).longValue();
            long loveCount = ((Number) row[16]).longValue();
            if (likeCount > 0) reactions.add(new ReactionSummary("LIKE", likeCount));
            if (dislikeCount > 0) reactions.add(new ReactionSummary("DISLIKE", dislikeCount));
            if (wowCount > 0) reactions.add(new ReactionSummary("WOW", wowCount));
            if (loveCount > 0) reactions.add(new ReactionSummary("LOVE", loveCount));

            String myReaction = (String) row[17];
            List<String> myReactionTypes = (myReaction != null && !myReaction.isEmpty())
                    ? List.of(myReaction) : Collections.emptyList();

            return new FeedResponse(feedId, userId, nickname, profileImageUrl,
                    resultId, generatedImageUrl, phrase, caption, isPublic, viewCount,
                    reactions, commentCount, myReactionTypes, createdAt, updatedAt);
        }).toList();

        BenchmarkResult result = recordResult("C: Native JOIN", stats, startNano);
        assertThat(responses).isNotEmpty();
        assertThat(result.queryCount()).isLessThanOrEqualTo(3); // 목표: 1~2 쿼리
    }

    // ═══════════════════════════════════════════
    // Strategy E: Cache-Control 헤더 검증
    // ═══════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("[Strategy E] Cache-Control — HTTP 캐시 헤더 검증")
    void strategyE_cacheControl() throws Exception {
        Statistics stats = resetStats();
        long startNano = System.nanoTime();

        mockMvc.perform(get("/api/v1/feeds")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().string("Cache-Control",
                        "max-age=30, stale-while-revalidate=60"));

        recordResult("E: Cache-Control", stats, startNano);
    }

    // ═══════════════════════════════════════════
    // 전체 결과 비교 테이블
    // ═══════════════════════════════════════════

    @Test
    @Order(99)
    @DisplayName("[Summary] 전체 전략 비교 테이블")
    void printComparisonTable() {
        System.out.println("\n");
        System.out.println("╔══════════════════════════════════╦════════════╦══════════════╦═══════════════╦═══════════════╗");
        System.out.println("║ Strategy                         ║ Query Count║ Entity Loads ║ Max Query(ms) ║ Response(ms)  ║");
        System.out.println("╠══════════════════════════════════╬════════════╬══════════════╬═══════════════╬═══════════════╣");
        for (BenchmarkResult r : results) {
            System.out.printf("║ %-32s ║ %10d ║ %12d ║ %13d ║ %13d ║%n",
                    r.strategy(), r.queryCount(), r.entityLoadCount(),
                    r.maxQueryTimeMs(), r.apiResponseTimeMs());
        }
        System.out.println("╚══════════════════════════════════╩════════════╩══════════════╩═══════════════╩═══════════════╝");
        System.out.println();
    }
}
