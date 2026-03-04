package com.link.vibe.domain.notification.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.config.TestMailConfig;
import com.link.vibe.config.TestRedisConfig;
import com.link.vibe.config.TestS3Config;
import com.link.vibe.domain.notification.entity.Notification;
import com.link.vibe.domain.notification.entity.NotificationType;
import com.link.vibe.domain.notification.repository.NotificationRepository;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.global.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestRedisConfig.class, TestS3Config.class, TestMailConfig.class })
@Transactional
class NotificationIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    EntityManager em;
    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    UserRepository userRepository;
    @Autowired
    NotificationRepository notificationRepository;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("noti-test@example.com")
                .password("password123")
                .nickname("notiuser")
                .name("Noti Test User")
                .build());

        accessToken = jwtTokenProvider.createAccessToken(
                testUser.getUserId(), testUser.getEmail());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Notification createNotification(NotificationType type, String title, String body, boolean isRead) {
        Notification notification = Notification.builder()
                .user(testUser)
                .type(type)
                .title(title)
                .body(body)
                .linkUrl("/test/" + type.name().toLowerCase())
                .referenceId(1L)
                .build();
        Notification saved = notificationRepository.save(notification);
        if (isRead) {
            saved.markAsRead();
        }
        em.flush();
        em.clear();
        return saved;
    }

    // ═══════════════════════════════════════════
    // GET /api/v1/notifications — 알림 목록 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/notifications - 알림 목록 조회")
    class GetNotifications {

        @Test
        @DisplayName("알림 목록을 조회할 수 있다")
        void success() throws Exception {
            createNotification(NotificationType.FOLLOW, "새 팔로워", "user1님이 팔로우했습니다", false);
            createNotification(NotificationType.FEED_COMMENT, "새 댓글", "user2님이 댓글을 남겼습니다", false);

            mockMvc.perform(get("/api/v1/notifications")
                    .header("Authorization", bearer(accessToken))
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("빈 목록을 조회할 수 있다")
        void empty() throws Exception {
            mockMvc.perform(get("/api/v1/notifications")
                    .header("Authorization", bearer(accessToken))
                    .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(0))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("커서 기반 페이지네이션이 동작한다")
        void cursorPagination() throws Exception {
            // n1, n3 변수는 직접 참조되지 않지만, 페이지네이션의 hasNext 여부와
            // 커서 이후의 데이터 존재를 검증하기 위해 데이터베이스에 생성되어야 합니다.
            createNotification(NotificationType.FOLLOW, "알림1", "본문1", false);
            Notification n2 = createNotification(NotificationType.FOLLOW, "알림2", "본문2", false);
            createNotification(NotificationType.FOLLOW, "알림3", "본문3", false);

            // size=2 → 최신 2개만, hasNext=true
            mockMvc.perform(get("/api/v1/notifications")
                    .header("Authorization", bearer(accessToken))
                    .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.hasNext").value(true));

            // cursor=n2 → n1만 반환, hasNext=false
            mockMvc.perform(get("/api/v1/notifications")
                    .header("Authorization", bearer(accessToken))
                    .param("size", "2")
                    .param("cursor", String.valueOf(n2.getNotificationId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }

        @Test
        @DisplayName("미인증 사용자는 조회할 수 없다 (401)")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/notifications")
                    .param("size", "20"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════
    // GET /api/v1/notifications/unread-count
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/notifications/unread-count - 읽지 않은 알림 수")
    class GetUnreadCount {

        @Test
        @DisplayName("읽지 않은 알림 수를 조회할 수 있다")
        void success() throws Exception {
            createNotification(NotificationType.FOLLOW, "알림1", "본문1", false);
            createNotification(NotificationType.FOLLOW, "알림2", "본문2", false);
            createNotification(NotificationType.FOLLOW, "알림3", "본문3", true); // 읽음

            mockMvc.perform(get("/api/v1/notifications/unread-count")
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(2));
        }

        @Test
        @DisplayName("알림이 없으면 0을 반환한다")
        void zero() throws Exception {
            mockMvc.perform(get("/api/v1/notifications/unread-count")
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.unreadCount").value(0));
        }
    }

    // ═══════════════════════════════════════════
    // PUT /api/v1/notifications/{id}/read
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/notifications/{id}/read - 알림 읽음 처리 (개별)")
    class MarkAsRead {

        @Test
        @DisplayName("알림을 읽음 처리할 수 있다")
        void success() throws Exception {
            Notification n = createNotification(NotificationType.FOLLOW, "새 팔로워", "본문", false);

            mockMvc.perform(put("/api/v1/notifications/{notificationId}/read", n.getNotificationId())
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isRead").value(true))
                    .andExpect(jsonPath("$.data.readAt").isNotEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 알림 읽음 처리 시 404")
        void notFound() throws Exception {
            mockMvc.perform(put("/api/v1/notifications/{notificationId}/read", 99999L)
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOTI_001"));
        }

        @Test
        @DisplayName("다른 사용자의 알림은 읽을 수 없다 (404)")
        void otherUserNotification() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .email("other@example.com")
                    .password("password123")
                    .nickname("otheruser")
                    .name("Other User")
                    .build());

            Notification otherNotification = notificationRepository.save(Notification.builder()
                    .user(otherUser)
                    .type(NotificationType.FOLLOW)
                    .title("알림")
                    .body("본문")
                    .build());
            em.flush();
            em.clear();

            mockMvc.perform(put("/api/v1/notifications/{notificationId}/read",
                    otherNotification.getNotificationId())
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOTI_001"));
        }
    }

    // ═══════════════════════════════════════════
    // PUT /api/v1/notifications/read-all
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("PUT /api/v1/notifications/read-all - 알림 전체 읽음")
    class MarkAllAsRead {

        @Test
        @DisplayName("모든 미읽 알림을 읽음 처리할 수 있다")
        void success() throws Exception {
            createNotification(NotificationType.FOLLOW, "알림1", "본문1", false);
            createNotification(NotificationType.FEED_COMMENT, "알림2", "본문2", false);
            createNotification(NotificationType.FEED_REACTION, "알림3", "본문3", true); // 이미 읽음

            mockMvc.perform(put("/api/v1/notifications/read-all")
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            em.flush();
            em.clear();
            long unreadCount = notificationRepository.countByUserUserIdAndIsReadFalse(testUser.getUserId());
            assertThat(unreadCount).isZero();
        }

        @Test
        @DisplayName("미읽 알림이 없어도 정상 응답한다")
        void noUnread() throws Exception {
            mockMvc.perform(put("/api/v1/notifications/read-all")
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ═══════════════════════════════════════════
    // DELETE /api/v1/notifications/{id}
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/notifications/{id} - 알림 삭제")
    class DeleteNotification {

        @Test
        @DisplayName("알림을 삭제할 수 있다")
        void success() throws Exception {
            Notification n = createNotification(NotificationType.FOLLOW, "삭제할 알림", "본문", false);
            Long notificationId = n.getNotificationId();

            mockMvc.perform(delete("/api/v1/notifications/{notificationId}", notificationId)
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            em.flush();
            em.clear();
            assertThat(notificationRepository.findById(notificationId)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 알림 삭제 시 404")
        void notFound() throws Exception {
            mockMvc.perform(delete("/api/v1/notifications/{notificationId}", 99999L)
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOTI_001"));
        }

        @Test
        @DisplayName("다른 사용자의 알림은 삭제할 수 없다 (404)")
        void otherUserNotification() throws Exception {
            User otherUser = userRepository.save(User.builder()
                    .email("other2@example.com")
                    .password("password123")
                    .nickname("otheruser2")
                    .name("Other User 2")
                    .build());

            Notification otherNotification = notificationRepository.save(Notification.builder()
                    .user(otherUser)
                    .type(NotificationType.FOLLOW)
                    .title("알림")
                    .body("본문")
                    .build());
            em.flush();
            em.clear();

            mockMvc.perform(delete("/api/v1/notifications/{notificationId}",
                    otherNotification.getNotificationId())
                    .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOTI_001"));
        }
    }
}
