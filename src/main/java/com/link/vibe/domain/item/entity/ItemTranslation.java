package com.link.vibe.domain.item.entity;

import com.link.vibe.domain.language.entity.Language;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item_translations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "language_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long translationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(nullable = false, length = 255)
    private String itemValue;

    @Column(columnDefinition = "TEXT")
    private String description;
}
