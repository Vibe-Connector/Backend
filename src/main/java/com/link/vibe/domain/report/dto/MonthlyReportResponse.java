package com.link.vibe.domain.report.dto;

import java.util.List;

public record MonthlyReportResponse(
        SummaryDto summary,
        SignatureDto signature,
        List<MoodKeywordStatDto> moodKeywords,
        List<WeeklyFlowDto> weeklyFlow,
        int[][] dailyHeatmap,
        List<TimeDistributionDto> timeDistribution,
        List<CategoryRecommendationDto> recommendations
) {

    public record SummaryDto(
            long totalVibes,
            long activeDays,
            double avgPerDay
    ) {}

    public record SignatureDto(
            String topMood,
            String topTime,
            String topSpace
    ) {}

    public record MoodKeywordStatDto(
            String word,
            long count,
            int percent
    ) {}

    public record WeeklyFlowDto(
            String week,
            long count
    ) {}

    public record TimeDistributionDto(
            String time,
            int percent
    ) {}

    public record CategoryRecommendationDto(
            String category,
            String icon,
            List<RecommendationItemDto> items
    ) {}

    public record RecommendationItemDto(
            String name,
            String match,
            int score
    ) {}
}
