package com.link.vibe.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movie_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovieDetail {

    @Id
    private Long itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(unique = true, nullable = false)
    private Integer tmdbId;

    @Column(length = 500)
    private String originalTitle;

    @Column(columnDefinition = "TEXT")
    private String overview;

    private LocalDate releaseDate;

    private Integer runtime;

    @Column(precision = 3, scale = 1)
    private BigDecimal voteAverage;

    private Integer voteCount;

    @Column(precision = 10, scale = 3)
    private BigDecimal popularity;

    @Column(length = 255)
    private String posterPath;

    @Column(columnDefinition = "JSON")
    private String genres;

    @Column(columnDefinition = "JSON")
    private String keywords;

    @Column(columnDefinition = "JSON")
    private String productionCountries;

    @Column(length = 10)
    private String originalLanguage;

    @Column(columnDefinition = "JSON")
    private String castInfo;

    @Column(columnDefinition = "JSON")
    private String releaseDates;

    @Column(length = 20)
    private String contentType;

    private LocalDateTime tmdbUpdatedAt;

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
