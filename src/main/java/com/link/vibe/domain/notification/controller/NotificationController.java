package com.link.vibe.domain.notification.controller;

import com.link.vibe.domain.notification.dto.NotificationResponse;
import com.link.vibe.domain.notification.dto.UnreadCountResponse;
import com.link.vibe.domain.notification.service.NotificationService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 API — 알림 목록 조회, 읽음 처리, 삭제 (커서 기반 페이지네이션)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    현재 사용자의 알림 목록을 최신순으로 조회합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    **페이지네이션:** 커서 기반 무한 스크롤. cursor 파라미터로 다음 페이지를 요청합니다.

                    **알림 유형:** FOLLOW, FEED_REACTION, FEED_COMMENT, VIBE_COMPLETE
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ApiResponse.ok(notificationService.getNotifications(
                userDetails.getUserId(), pageRequest));
    }

    @Operation(
            summary = "읽지 않은 알림 수 조회",
            description = """
                    읽지 않은 알림의 총 개수를 조회합니다 (뱃지 표시용).

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(notificationService.getUnreadCount(userDetails.getUserId()));
    }

    @Operation(
            summary = "알림 읽음 처리 (개별)",
            description = """
                    특정 알림을 읽음 상태로 변경합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    본인의 알림만 읽음 처리할 수 있습니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 404 (NOTI_001): 알림을 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PutMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "알림 ID", example = "1") @PathVariable Long notificationId) {
        return ApiResponse.ok(notificationService.markAsRead(
                userDetails.getUserId(), notificationId));
    }

    @Operation(
            summary = "알림 전체 읽음 처리",
            description = """
                    현재 사용자의 읽지 않은 알림을 모두 읽음 상태로 변경합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    읽지 않은 알림이 없어도 정상 응답(200)을 반환합니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "전체 읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUserId());
        return ApiResponse.ok(null);
    }

    @Operation(
            summary = "알림 삭제",
            description = """
                    알림을 삭제합니다 (하드 삭제).

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.

                    본인의 알림만 삭제할 수 있습니다.

                    **에러:**
                    - 401 (AUTH_001): 인증되지 않은 요청
                    - 404 (NOTI_001): 알림을 찾을 수 없음
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "알림 ID", example = "1") @PathVariable Long notificationId) {
        notificationService.deleteNotification(userDetails.getUserId(), notificationId);
        return ApiResponse.ok(null);
    }
}
