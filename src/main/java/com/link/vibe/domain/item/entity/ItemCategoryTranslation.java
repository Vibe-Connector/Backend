package com.link.vibe.domain.item.entity;

import com.link.vibe.domain.language.entity.Language;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item_category_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "language_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemCategoryTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long translationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(nullable = false, length = 100)
    private String categoryValue;

    @Column(columnDefinition = "TEXT")
    private String description;
}
