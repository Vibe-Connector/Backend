package com.link.vibe.domain.vibe.entity;

import com.link.vibe.domain.option.entity.MoodKeyword;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "vibe_request_moods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"vibe_id", "keyword_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VibeRequestMood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vibe_id", nullable = false)
    private VibeRequest vibeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private MoodKeyword moodKeyword;

    public VibeRequestMood(MoodKeyword moodKeyword) {
        this.moodKeyword = moodKeyword;
    }

    void setVibeRequest(VibeRequest vibeRequest) {
        this.vibeRequest = vibeRequest;
    }
}
