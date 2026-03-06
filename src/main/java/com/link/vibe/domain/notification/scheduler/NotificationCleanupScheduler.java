package com.link.vibe.domain.notification.scheduler;

import com.link.vibe.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    /**
     * 매일 새벽 3시에 읽음 처리 후 30일이 지난 알림을 자동 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupReadNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        int deleted = notificationRepository.deleteReadNotificationsBefore(threshold);
        log.info("읽음 알림 자동 삭제: {}건 (기준: {} 이전)", deleted, threshold);
    }
}
