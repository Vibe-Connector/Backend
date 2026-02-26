package com.link.vibe.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled;

    @Column(name = "email_notification", nullable = false)
    private Boolean emailNotification;

    @Column(name = "default_share_privacy", nullable = false, length = 10)
    private String defaultSharePrivacy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public UserSettings(User user) {
        this.user = user;
        this.pushEnabled = true;
        this.emailNotification = true;
        this.defaultSharePrivacy = "PRIVATE";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(Boolean pushEnabled, Boolean emailNotification, String defaultSharePrivacy) {
        if (pushEnabled != null) this.pushEnabled = pushEnabled;
        if (emailNotification != null) this.emailNotification = emailNotification;
        if (defaultSharePrivacy != null) this.defaultSharePrivacy = defaultSharePrivacy;
    }
}
