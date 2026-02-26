package com.link.vibe.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "nickname", unique = true, length = 50)
    private String nickname;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "preferred_language_id")
    private Long preferredLanguageId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public User(String email, String password, String nickname, String name, String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.status = "ACTIVE";
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

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(String nickname, String name, String gender, Integer birthYear,
                              String profileImageUrl, Long preferredLanguageId) {
        if (nickname != null) this.nickname = nickname;
        if (name != null) this.name = name;
        if (gender != null) this.gender = gender;
        if (birthYear != null) this.birthYear = birthYear;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (preferredLanguageId != null) this.preferredLanguageId = preferredLanguageId;
    }
}
