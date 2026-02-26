package com.link.vibe.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_id")
    private Long socialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public SocialAccount(User user, String provider, String providerUserId,
                         String accessToken, String refreshToken, LocalDateTime tokenExpiresAt) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateTokens(String accessToken, String refreshToken, LocalDateTime tokenExpiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
    }
}
