package com.link.vibe.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "music_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MusicDetail {

    @Id
    private Long itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(unique = true, length = 36)
    private String musicbrainzId;

    @Column(length = 12)
    private String isrc;

    @Column(nullable = false, columnDefinition = "JSON")
    private String artists;

    @Column(length = 500)
    private String albumName;

    @Column(length = 500)
    private String albumCoverUrl;

    private Integer trackDurationMs;

    private LocalDate releaseDate;

    @Column(columnDefinition = "JSON")
    private String genres;

    private Integer deezerId;

    @Column(length = 500)
    private String previewUrl;

    @Column(length = 100)
    private String spotifyUri;

    @Column(columnDefinition = "TEXT")
    private String lyrics;

    @Column(length = 20)
    private String contentType;

    private LocalDateTime sourceUpdatedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
