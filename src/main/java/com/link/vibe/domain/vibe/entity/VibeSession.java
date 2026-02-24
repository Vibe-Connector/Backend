package com.link.vibe.domain.vibe.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vibe_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VibeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToOne(mappedBy = "vibeSession", fetch = FetchType.LAZY)
    private VibePrompt vibePrompt;

    @OneToOne(mappedBy = "vibeSession", fetch = FetchType.LAZY)
    private VibeResult vibeResult;

    @Builder
    public VibeSession(Long userId) {
        this.userId = userId;
        this.status = "IN_PROGRESS";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }
}
