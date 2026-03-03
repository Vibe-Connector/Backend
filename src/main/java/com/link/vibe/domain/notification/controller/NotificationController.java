package com.link.vibe.domain.notification.controller;

import com.link.vibe.domain.notification.dto.NotificationResponse;
import com.link.vibe.domain.notification.dto.UnreadCountResponse;
import com.link.vibe.domain.notification.service.NotificationService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "커서 기반 페이지네이션으로 알림 목록을 조회합니다")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ApiResponse.ok(notificationService.getNotifications(
                userDetails.getUserId(), pageRequest));
    }

    @Operation(summary = "읽지 않은 알림 수 조회")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(notificationService.getUnreadCount(userDetails.getUserId()));
    }

    @Operation(summary = "알림 읽음 처리 (개별)")
    @PutMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        return ApiResponse.ok(notificationService.markAsRead(
                userDetails.getUserId(), notificationId));
    }

    @Operation(summary = "알림 전체 읽음 처리")
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUserId());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "알림 삭제")
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationService.deleteNotification(userDetails.getUserId(), notificationId);
        return ApiResponse.ok(null);
    }
}
