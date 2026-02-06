package com.link.vibe.domain.vibe.entity;

import com.link.vibe.domain.option.entity.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vibe_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VibeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vibe_id")
    private Long vibeId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_id")
    private TimeOption timeOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weather_id")
    private WeatherOption weatherOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private PlaceOption placeOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "companion_id")
    private CompanionOption companionOption;

    @Column(name = "final_prompt", columnDefinition = "TEXT")
    private String finalPrompt;

    @Column(name = "result_phrase", columnDefinition = "TEXT")
    private String resultPhrase;

    @Column(name = "result_analysis", columnDefinition = "TEXT")
    private String resultAnalysis;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "vibeRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VibeRequestMood> moods = new ArrayList<>();

    @Builder
    public VibeRequest(String sessionId, TimeOption timeOption, WeatherOption weatherOption,
                       PlaceOption placeOption, CompanionOption companionOption) {
        this.sessionId = sessionId;
        this.timeOption = timeOption;
        this.weatherOption = weatherOption;
        this.placeOption = placeOption;
        this.companionOption = companionOption;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void addMood(VibeRequestMood mood) {
        this.moods.add(mood);
        mood.setVibeRequest(this);
    }

    public void applyResult(String finalPrompt, String resultPhrase, String resultAnalysis, int processingTimeMs) {
        this.finalPrompt = finalPrompt;
        this.resultPhrase = resultPhrase;
        this.resultAnalysis = resultAnalysis;
        this.processingTimeMs = processingTimeMs;
    }
}
