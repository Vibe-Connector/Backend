package com.link.vibe.domain.vibe.entity;

import com.link.vibe.domain.item.entity.Item;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vibe_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"result_id", "item_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VibeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vibe_item_id")
    private Long vibeItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private VibeResult vibeResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "match_score", precision = 5, scale = 2)
    private BigDecimal matchScore;

    @Column(name = "recommend_reason", columnDefinition = "TEXT")
    private String recommendReason;

    @Column(name = "is_user_liked")
    private Boolean isUserLiked;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public VibeItem(VibeResult vibeResult, Item item, BigDecimal matchScore,
                    String recommendReason) {
        this.vibeResult = vibeResult;
        this.item = item;
        this.matchScore = matchScore;
        this.recommendReason = recommendReason;
        this.isUserLiked = false;
    }

    public void toggleLike() {
        this.isUserLiked = !Boolean.TRUE.equals(this.isUserLiked);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
