package com.link.vibe.domain.vibe.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vibe_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VibeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private VibeSession vibeSession;

    @Column(name = "generated_image_url", length = 500)
    private String generatedImageUrl;

    @Column(name = "phrase", columnDefinition = "TEXT")
    private String phrase;

    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column(name = "ai_model_version", length = 50)
    private String aiModelVersion;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "vibeResult", cascade = CascadeType.ALL)
    private List<VibeItem> vibeItems = new ArrayList<>();

    @Builder
    public VibeResult(VibeSession vibeSession, String phrase, String aiAnalysis,
                      String aiModelVersion, Integer processingTimeMs) {
        this.vibeSession = vibeSession;
        this.phrase = phrase;
        this.aiAnalysis = aiAnalysis;
        this.aiModelVersion = aiModelVersion;
        this.processingTimeMs = processingTimeMs;
    }

    public void updateGeneratedImageUrl(String imageUrl) {
        this.generatedImageUrl = imageUrl;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
