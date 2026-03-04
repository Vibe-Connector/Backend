package com.link.vibe.domain.report.dto;

import java.util.List;

public record YearlyReportResponse(
        YearlySummaryDto summary,
        List<MonthlyTrendDto> monthlyTrend,
        List<QuarterlyEvolutionDto> quarterlyEvolution,
        List<MoodRatioDto> moodDistribution,
        List<HighlightDto> highlights,
        List<BestMatchingDto> bestMatching
) {

    public record YearlySummaryDto(
            long totalVibes,
            long activeDays,
            double avgPerMonth
    ) {}

    public record MonthlyTrendDto(
            String month,
            long count,
            String topMood,
            String color
    ) {}

    public record QuarterlyEvolutionDto(
            String quarter,
            List<String> moods,
            String theme,
            String color
    ) {}

    public record MoodRatioDto(
            String mood,
            int percent
    ) {}

    public record HighlightDto(
            String icon,
            String title,
            String value,
            String detail
    ) {}

    public record BestMatchingDto(
            String category,
            String icon,
            String item,
            String reason,
            long score
    ) {}
}
