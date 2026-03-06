package com.link.vibe.domain.notification.scheduler;

import com.link.vibe.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationCleanupScheduler 단위 테스트")
class NotificationCleanupSchedulerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCleanupScheduler scheduler;

    @Test
    @DisplayName("30일 이전 기준으로 deleteReadNotificationsBefore를 호출한다")
    void cleanupReadNotifications_callsWithCorrectThreshold() {
        when(notificationRepository.deleteReadNotificationsBefore(any(LocalDateTime.class)))
                .thenReturn(5);

        scheduler.cleanupReadNotifications();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificationRepository).deleteReadNotificationsBefore(captor.capture());

        LocalDateTime threshold = captor.getValue();
        LocalDateTime expected = LocalDateTime.now().minusDays(30);
        // 실행 시간 차이를 1분 이내로 허용
        assertThat(ChronoUnit.MINUTES.between(threshold, expected)).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("삭제된 건수가 0이어도 정상 실행된다")
    void cleanupReadNotifications_zeroDeletions() {
        when(notificationRepository.deleteReadNotificationsBefore(any(LocalDateTime.class)))
                .thenReturn(0);

        scheduler.cleanupReadNotifications();

        verify(notificationRepository).deleteReadNotificationsBefore(any(LocalDateTime.class));
    }
}
