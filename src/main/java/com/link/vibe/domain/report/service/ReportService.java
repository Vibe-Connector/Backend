package com.link.vibe.domain.report.service;

import com.link.vibe.domain.report.dto.MonthlyReportResponse;
import com.link.vibe.domain.report.dto.MonthlyReportResponse.*;
import com.link.vibe.domain.report.dto.YearlyReportResponse;
import com.link.vibe.domain.report.dto.YearlyReportResponse.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    @PersistenceContext
    private final EntityManager em;

    // ── 월간 리포트 ──

    public MonthlyReportResponse getMonthlyReport(Long userId, int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59);

        SummaryDto summary = buildMonthlySummary(userId, start, end);
        SignatureDto signature = buildSignature(userId, start, end);
        List<MoodKeywordStatDto> moodKeywords = buildMoodKeywords(userId, start, end);
        List<WeeklyFlowDto> weeklyFlow = buildWeeklyFlow(userId, start, end, year, month);
        int[][] dailyHeatmap = buildDailyHeatmap(userId, year, month);
        List<TimeDistributionDto> timeDistribution = buildTimeDistribution(userId, start, end);
        List<CategoryRecommendationDto> recommendations = buildRecommendations(userId, start, end);

        return new MonthlyReportResponse(
                summary, signature, moodKeywords, weeklyFlow,
                dailyHeatmap, timeDistribution, recommendations
        );
    }

    // ── 연간 리포트 ──

    public YearlyReportResponse getYearlyReport(Long userId, int year) {
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        YearlySummaryDto summary = buildYearlySummary(userId, start, end);
        List<MonthlyTrendDto> monthlyTrend = buildMonthlyTrend(userId, year);
        List<QuarterlyEvolutionDto> quarterlyEvolution = buildQuarterlyEvolution(userId, year);
        List<MoodRatioDto> moodDistribution = buildYearlyMoodDistribution(userId, start, end);
        List<HighlightDto> highlights = buildHighlights(userId, year, start, end);
        List<BestMatchingDto> bestMatching = buildBestMatching(userId, start, end);

        return new YearlyReportResponse(
                summary, monthlyTrend, quarterlyEvolution,
                moodDistribution, highlights, bestMatching
        );
    }

    // ── Private: 월간 빌더 메서드 ──

    private SummaryDto buildMonthlySummary(Long userId, LocalDateTime start, LocalDateTime end) {
        long totalVibes = countCompletedSessions(userId, start, end);
        long activeDays = countActiveDays(userId, start, end);
        double avgPerDay = activeDays > 0 ? Math.round((double) totalVibes / activeDays * 10.0) / 10.0 : 0;
        return new SummaryDto(totalVibes, activeDays, avgPerDay);
    }

    private SignatureDto buildSignature(Long userId, LocalDateTime start, LocalDateTime end) {
        String topMood = getTopMoodKeyword(userId, start, end);
        String topTime = getTopTimeOption(userId, start, end);
        String topSpace = getTopPlaceOption(userId, start, end);
        return new SignatureDto(topMood, topTime, topSpace);
    }

    @SuppressWarnings("unchecked")
    private List<MoodKeywordStatDto> buildMoodKeywords(Long userId, LocalDateTime start, LocalDateTime end) {
        // mood_keyword_ids는 JSON 배열 (예: [1, 3, 7])
        // PostgreSQL jsonb_array_elements_text로 파싱 후 집계
        List<Object[]> rows = em.createNativeQuery("""
                SELECT mk.keyword_value, COUNT(*) as cnt
                FROM vibe_prompts vp
                JOIN vibe_sessions vs ON vp.session_id = vs.session_id
                CROSS JOIN LATERAL jsonb_array_elements_text(vp.mood_keyword_ids) AS kid
                JOIN mood_keywords mk ON mk.keyword_id = CAST(kid AS BIGINT)
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY mk.keyword_value
                ORDER BY cnt DESC
                LIMIT 5
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        return rows.stream()
                .map(r -> new MoodKeywordStatDto(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        total > 0 ? (int) Math.round(((Number) r[1]).longValue() * 100.0 / total) : 0
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<WeeklyFlowDto> buildWeeklyFlow(Long userId, LocalDateTime start, LocalDateTime end, int year, int month) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT EXTRACT(WEEK FROM vs.created_at) AS wk, COUNT(*) AS cnt
                FROM vibe_sessions vs
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY wk
                ORDER BY wk
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        List<WeeklyFlowDto> result = new ArrayList<>();
        int weekNum = 1;
        for (Object[] r : rows) {
            result.add(new WeeklyFlowDto(weekNum + "주차", ((Number) r[1]).longValue()));
            weekNum++;
        }
        return result;
    }

    private int[][] buildDailyHeatmap(Long userId, int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT CAST(vs.created_at AS DATE) AS d, COUNT(*) AS cnt
                FROM vibe_sessions vs
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY d
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        Map<LocalDate, Long> countByDate = new HashMap<>();
        for (Object[] r : rows) {
            LocalDate date = ((java.sql.Date) r[0]).toLocalDate();
            countByDate.put(date, ((Number) r[1]).longValue());
        }

        // 5x7 그리드 생성 (주×요일)
        int[][] heatmap = new int[5][7];
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int startDayOfWeek = firstDay.getDayOfWeek().getValue() - 1; // 월=0

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            int pos = startDayOfWeek + day - 1;
            int week = pos / 7;
            int dow = pos % 7;
            if (week < 5) {
                long count = countByDate.getOrDefault(date, 0L);
                // 0~4 스케일로 변환
                heatmap[week][dow] = count == 0 ? 0 : Math.min((int) count, 4);
            }
        }
        return heatmap;
    }

    @SuppressWarnings("unchecked")
    private List<TimeDistributionDto> buildTimeDistribution(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT to2.time_key, COUNT(*) AS cnt
                FROM vibe_prompts vp
                JOIN vibe_sessions vs ON vp.session_id = vs.session_id
                JOIN time_options to2 ON vp.time_id = to2.time_id
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY to2.time_key, to2.time_id
                ORDER BY to2.time_id
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        return rows.stream()
                .map(r -> new TimeDistributionDto(
                        (String) r[0],
                        total > 0 ? (int) Math.round(((Number) r[1]).longValue() * 100.0 / total) : 0
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<CategoryRecommendationDto> buildRecommendations(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT ic.category_key, it.item_key, COUNT(*) AS cnt
                FROM vibe_items vi
                JOIN vibe_results vr ON vi.result_id = vr.result_id
                JOIN vibe_sessions vs ON vr.session_id = vs.session_id
                JOIN items it ON vi.item_id = it.item_id
                JOIN item_categories ic ON it.category_id = ic.category_id
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY ic.category_key, it.item_key
                ORDER BY ic.category_key, cnt DESC
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        Map<String, String> categoryIcons = Map.of(
                "coffee", "\u2615", "music", "\uD83C\uDFB5",
                "movie", "\uD83C\uDFAC", "lighting", "\uD83D\uDCA1"
        );

        // 카테고리별 top 3
        Map<String, List<Object[]>> grouped = new LinkedHashMap<>();
        for (Object[] r : rows) {
            grouped.computeIfAbsent((String) r[0], k -> new ArrayList<>()).add(r);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    String cat = entry.getKey();
                    List<RecommendationItemDto> items = entry.getValue().stream()
                            .limit(3)
                            .map(r -> new RecommendationItemDto(
                                    (String) r[1], "", ((Number) r[2]).intValue()
                            ))
                            .toList();
                    return new CategoryRecommendationDto(cat, categoryIcons.getOrDefault(cat, ""), items);
                })
                .toList();
    }

    // ── Private: 연간 빌더 메서드 ──

    private YearlySummaryDto buildYearlySummary(Long userId, LocalDateTime start, LocalDateTime end) {
        long totalVibes = countCompletedSessions(userId, start, end);
        long activeDays = countActiveDays(userId, start, end);
        double avgPerMonth = Math.round(totalVibes / 12.0 * 10.0) / 10.0;
        return new YearlySummaryDto(totalVibes, activeDays, avgPerMonth);
    }

    @SuppressWarnings("unchecked")
    private List<MonthlyTrendDto> buildMonthlyTrend(Long userId, int year) {
        String[] monthColors = {
                "#93C5FD", "#FCA5A5", "#A7F3D0", "#FDE68A", "#6EE7B7", "#F1863B",
                "#67E8F9", "#FDBA74", "#C4B5FD", "#F9A8D4", "#E85D75", "#F1863B"
        };

        List<MonthlyTrendDto> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            LocalDateTime mStart = LocalDateTime.of(year, m, 1, 0, 0);
            LocalDateTime mEnd = YearMonth.of(year, m).atEndOfMonth().atTime(23, 59, 59);
            long count = countCompletedSessions(userId, mStart, mEnd);
            String topMood = count > 0 ? getTopMoodKeyword(userId, mStart, mEnd) : "";
            result.add(new MonthlyTrendDto(m + "월", count, topMood, monthColors[m - 1]));
        }
        return result;
    }

    private List<QuarterlyEvolutionDto> buildQuarterlyEvolution(Long userId, int year) {
        String[] themes = {"새로운 시작", "에너지 충전", "감성 회복", "따뜻한 마무리"};
        String[] colors = {"#93C5FD", "#FDE68A", "#67E8F9", "#F1863B"};

        List<QuarterlyEvolutionDto> result = new ArrayList<>();
        for (int q = 0; q < 4; q++) {
            int startMonth = q * 3 + 1;
            LocalDateTime qStart = LocalDateTime.of(year, startMonth, 1, 0, 0);
            LocalDateTime qEnd = YearMonth.of(year, startMonth + 2).atEndOfMonth().atTime(23, 59, 59);

            List<String> moods = getTopNMoods(userId, qStart, qEnd, 3);
            result.add(new QuarterlyEvolutionDto("Q" + (q + 1), moods, themes[q], colors[q]));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<MoodRatioDto> buildYearlyMoodDistribution(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT mk.keyword_value, COUNT(*) as cnt
                FROM vibe_prompts vp
                JOIN vibe_sessions vs ON vp.session_id = vs.session_id
                CROSS JOIN LATERAL jsonb_array_elements_text(vp.mood_keyword_ids) AS kid
                JOIN mood_keywords mk ON mk.keyword_id = CAST(kid AS BIGINT)
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY mk.keyword_value
                ORDER BY cnt DESC
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<MoodRatioDto> result = new ArrayList<>();
        int accPercent = 0;

        for (int i = 0; i < Math.min(rows.size(), 5); i++) {
            Object[] r = rows.get(i);
            int pct = total > 0 ? (int) Math.round(((Number) r[1]).longValue() * 100.0 / total) : 0;
            accPercent += pct;
            result.add(new MoodRatioDto((String) r[0], pct));
        }

        // 기타 항목
        if (rows.size() > 5 && total > 0) {
            result.add(new MoodRatioDto("기타", 100 - accPercent));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<HighlightDto> buildHighlights(Long userId, int year, LocalDateTime start, LocalDateTime end) {
        List<HighlightDto> highlights = new ArrayList<>();

        // 가장 활발했던 달
        List<Object[]> monthCounts = em.createNativeQuery("""
                SELECT EXTRACT(MONTH FROM vs.created_at) AS m, COUNT(*) AS cnt
                FROM vibe_sessions vs
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY m ORDER BY cnt DESC LIMIT 1
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        if (!monthCounts.isEmpty()) {
            Object[] top = monthCounts.get(0);
            int m = ((Number) top[0]).intValue();
            long cnt = ((Number) top[1]).longValue();
            highlights.add(new HighlightDto("\uD83C\uDFC6", "가장 활발했던 달", m + "월", cnt + "개 Vibe 생성"));
        }

        // 총 생성 수
        long totalVibes = countCompletedSessions(userId, start, end);
        highlights.add(new HighlightDto("\u2728", "총 Vibe 생성", totalVibes + "개", year + "년 전체"));

        // 연속 사용 스트릭
        List<Object[]> dates = em.createNativeQuery("""
                SELECT DISTINCT CAST(vs.created_at AS DATE) AS d
                FROM vibe_sessions vs
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                ORDER BY d
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        int maxStreak = 0, currentStreak = 1;
        for (int i = 1; i < dates.size(); i++) {
            LocalDate prev = ((java.sql.Date) dates.get(i - 1)[0]).toLocalDate();
            LocalDate curr = ((java.sql.Date) dates.get(i)[0]).toLocalDate();
            if (curr.equals(prev.plusDays(1))) {
                currentStreak++;
            } else {
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            }
        }
        maxStreak = Math.max(maxStreak, currentStreak);
        highlights.add(new HighlightDto("\uD83D\uDD25", "최장 연속 사용", maxStreak + "일", "연속 기록"));

        // 활동 일수
        long activeDays = countActiveDays(userId, start, end);
        highlights.add(new HighlightDto("\uD83D\uDCC5", "활동한 날", activeDays + "일", "365일 중"));

        return highlights;
    }

    @SuppressWarnings("unchecked")
    private List<BestMatchingDto> buildBestMatching(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT ic.category_key, it.item_key, COUNT(*) AS cnt
                FROM vibe_items vi
                JOIN vibe_results vr ON vi.result_id = vr.result_id
                JOIN vibe_sessions vs ON vr.session_id = vs.session_id
                JOIN items it ON vi.item_id = it.item_id
                JOIN item_categories ic ON it.category_id = ic.category_id
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY ic.category_key, it.item_key
                ORDER BY ic.category_key, cnt DESC
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();

        Map<String, String> categoryIcons = Map.of(
                "coffee", "\u2615", "music", "\uD83C\uDFB5",
                "movie", "\uD83C\uDFAC", "lighting", "\uD83D\uDCA1"
        );

        // 카테고리별 top 1
        Map<String, Object[]> topByCategory = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String cat = (String) r[0];
            topByCategory.putIfAbsent(cat, r);
        }

        return topByCategory.entrySet().stream()
                .map(entry -> {
                    String cat = entry.getKey();
                    Object[] r = entry.getValue();
                    return new BestMatchingDto(
                            cat,
                            categoryIcons.getOrDefault(cat, ""),
                            (String) r[1],
                            "가장 많이 추천된 아이템",
                            ((Number) r[2]).longValue()
                    );
                })
                .toList();
    }

    // ── Private: 공통 헬퍼 ──

    private long countCompletedSessions(Long userId, LocalDateTime start, LocalDateTime end) {
        return ((Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM vibe_sessions
                WHERE user_id = :userId AND status = 'COMPLETED'
                  AND created_at BETWEEN :start AND :end
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult()).longValue();
    }

    private long countActiveDays(Long userId, LocalDateTime start, LocalDateTime end) {
        return ((Number) em.createNativeQuery("""
                SELECT COUNT(DISTINCT CAST(created_at AS DATE))
                FROM vibe_sessions
                WHERE user_id = :userId AND status = 'COMPLETED'
                  AND created_at BETWEEN :start AND :end
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private String getTopMoodKeyword(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT mk.keyword_value, COUNT(*) as cnt
                FROM vibe_prompts vp
                JOIN vibe_sessions vs ON vp.session_id = vs.session_id
                CROSS JOIN LATERAL jsonb_array_elements_text(vp.mood_keyword_ids) AS kid
                JOIN mood_keywords mk ON mk.keyword_id = CAST(kid AS BIGINT)
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY mk.keyword_value
                ORDER BY cnt DESC
                LIMIT 1
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        return rows.isEmpty() ? "" : (String) rows.get(0)[0];
    }

    @SuppressWarnings("unchecked")
    private List<String> getTopNMoods(Long userId, LocalDateTime start, LocalDateTime end, int n) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT mk.keyword_value, COUNT(*) as cnt
                FROM vibe_prompts vp
                JOIN vibe_sessions vs ON vp.session_id = vs.session_id
                CROSS JOIN LATERAL jsonb_array_elements_text(vp.mood_keyword_ids) AS kid
                JOIN mood_keywords mk ON mk.keyword_id = CAST(kid AS BIGINT)
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY mk.keyword_value
                ORDER BY cnt DESC
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .setMaxResults(n)
                .getResultList();
        return rows.stream().map(r -> (String) r[0]).toList();
    }

    @SuppressWarnings("unchecked")
    private String getTopTimeOption(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT to2.time_key, COUNT(*) AS cnt
                FROM vibe_prompts vp
                JOIN vibe_sessions vs ON vp.session_id = vs.session_id
                JOIN time_options to2 ON vp.time_id = to2.time_id
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY to2.time_key
                ORDER BY cnt DESC
                LIMIT 1
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        return rows.isEmpty() ? "" : (String) rows.get(0)[0];
    }

    @SuppressWarnings("unchecked")
    private String getTopPlaceOption(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT po.place_key, COUNT(*) AS cnt
                FROM vibe_prompts vp
                JOIN vibe_sessions vs ON vp.session_id = vs.session_id
                JOIN place_options po ON vp.place_id = po.place_id
                WHERE vs.user_id = :userId AND vs.status = 'COMPLETED'
                  AND vs.created_at BETWEEN :start AND :end
                GROUP BY po.place_key
                ORDER BY cnt DESC
                LIMIT 1
                """)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        return rows.isEmpty() ? "" : (String) rows.get(0)[0];
    }
}
