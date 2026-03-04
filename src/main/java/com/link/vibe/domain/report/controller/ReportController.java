package com.link.vibe.domain.report.controller;

import com.link.vibe.domain.report.dto.MonthlyReportResponse;
import com.link.vibe.domain.report.dto.YearlyReportResponse;
import com.link.vibe.domain.report.service.ReportService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Report", description = "분석 리포트 API")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "월간 분석 리포트 조회",
            description = """
                    지정한 연/월의 월간 분석 리포트를 조회합니다.
                    핵심 지표, 시그니처, 무드 키워드, 주간 흐름, 히트맵, 시간대 분포, 추천 데이터를 반환합니다.
                    """
    )
    @GetMapping("/monthly/{year}/{month}")
    public ApiResponse<MonthlyReportResponse> getMonthlyReport(
            @PathVariable int year,
            @PathVariable int month) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(reportService.getMonthlyReport(userId, year, month));
    }

    @Operation(
            summary = "연간 분석 리포트 조회",
            description = """
                    지정한 연도의 연간 분석 리포트를 조회합니다.
                    월별 추세, 분기별 변화, 무드 비율, 하이라이트, 베스트 매칭 데이터를 반환합니다.
                    """
    )
    @GetMapping("/yearly/{year}")
    public ApiResponse<YearlyReportResponse> getYearlyReport(@PathVariable int year) {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(reportService.getYearlyReport(userId, year));
    }
}
