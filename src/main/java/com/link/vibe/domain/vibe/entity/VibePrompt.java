package com.link.vibe.domain.vibe.entity;

import com.link.vibe.domain.option.entity.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "vibe_prompts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VibePrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prompt_id")
    private Long promptId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private VibeSession vibeSession;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mood_keyword_ids", columnDefinition = "jsonb")
    private String moodKeywordIds;

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public VibePrompt(VibeSession vibeSession, String moodKeywordIds,
                      TimeOption timeOption, WeatherOption weatherOption,
                      PlaceOption placeOption, CompanionOption companionOption,
                      String finalPrompt) {
        this.vibeSession = vibeSession;
        this.moodKeywordIds = moodKeywordIds;
        this.timeOption = timeOption;
        this.weatherOption = weatherOption;
        this.placeOption = placeOption;
        this.companionOption = companionOption;
        this.finalPrompt = finalPrompt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateFinalPrompt(String finalPrompt) {
        this.finalPrompt = finalPrompt;
    }
}
