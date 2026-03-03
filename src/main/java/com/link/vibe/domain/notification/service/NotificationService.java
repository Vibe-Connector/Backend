package com.link.vibe.domain.notification.service;

import com.link.vibe.domain.notification.dto.NotificationResponse;
import com.link.vibe.domain.notification.dto.UnreadCountResponse;
import com.link.vibe.domain.notification.entity.Notification;
import com.link.vibe.domain.notification.entity.NotificationType;
import com.link.vibe.domain.notification.repository.NotificationRepository;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ──── 알림 조회 ────

    public PageResponse<NotificationResponse> getNotifications(
            Long userId, CursorPageRequest pageRequest) {

        Pageable pageable = PageRequest.of(0, pageRequest.getFetchSize());
        List<Notification> notifications;

        if (pageRequest.hasCursor()) {
            Long cursor = Long.parseLong(pageRequest.getCursor());
            notifications = notificationRepository.findByUserWithCursor(userId, cursor, pageable);
        } else {
            notifications = notificationRepository.findByUser(userId, pageable);
        }

        List<NotificationResponse> content = notifications.stream()
                .map(NotificationResponse::of)
                .toList();

        return PageResponse.of(content, pageRequest.getEffectiveSize(),
                n -> String.valueOf(n.notificationId()));
    }

    public UnreadCountResponse getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserUserIdAndIsReadFalse(userId);
        return new UnreadCountResponse(count);
    }

    // ──── 알림 읽음 처리 ────

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUserUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
        return NotificationResponse.of(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        int updatedCount = notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        log.info("전체 읽음 처리 완료: userId={}, updatedCount={}", userId, updatedCount);
    }

    // ──── 알림 삭제 ────

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUserUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notificationRepository.delete(notification);
    }

    // ──── 알림 생성 (EventListener에서 호출) ────

    @Transactional
    public void create(Long recipientUserId, NotificationType type,
            String title, String body, String linkUrl, Long referenceId) {
        User user = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .linkUrl(linkUrl)
                .referenceId(referenceId)
                .build();

        notificationRepository.save(notification);
        log.info("알림 생성: userId={}, type={}, title={}", recipientUserId, type, title);
    }
}
