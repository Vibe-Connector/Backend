package com.link.vibe.domain.notification.repository;

import com.link.vibe.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 알림 목록 (커서 기반 페이지네이션, 최신순)
    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.userId = :userId " +
            "AND n.notificationId < :cursor " +
            "ORDER BY n.notificationId DESC")
    List<Notification> findByUserWithCursor(@Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.userId = :userId " +
            "ORDER BY n.notificationId DESC")
    List<Notification> findByUser(@Param("userId") Long userId, Pageable pageable);

    // 소유자 확인 포함 단건 조회
    Optional<Notification> findByNotificationIdAndUserUserId(Long notificationId, Long userId);

    // 읽지 않은 알림 수
    long countByUserUserIdAndIsReadFalse(Long userId);

    // 전체 읽음 처리용 (벌크 UPDATE)
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
            "WHERE n.user.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
