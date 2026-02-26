package com.link.vibe.domain.item.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coffee_details")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeDetail {

    @Id
    private Long itemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(unique = true, nullable = false, length = 100)
    private String capsuleKey;

    @Column(nullable = false, length = 255)
    private String capsuleName;

    @Column(nullable = false, length = 50)
    private String line;

    @Column(length = 100)
    private String subCategory;

    private Integer intensity;

    private Integer intensityMax;

    @Column(nullable = false, columnDefinition = "JSON")
    private String cupSizes;

    @Column(length = 100)
    private String beanType;

    @Column(columnDefinition = "JSON")
    private String origins;

    @Column(length = 100)
    private String roastLevel;

    @Column(columnDefinition = "JSON")
    private String aromaProfile;

    @Column(columnDefinition = "TEXT")
    private String flavorNotes;

    private Integer body;

    private Integer bitterness;

    private Integer acidity;

    private Integer roasting;

    private Boolean isDecaf;

    private Boolean isLimitedEdition;

    private Integer pricePerCapsuleKrw;

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
