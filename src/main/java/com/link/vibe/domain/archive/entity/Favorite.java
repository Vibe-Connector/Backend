package com.link.vibe.domain.archive.entity;

import com.link.vibe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_archive", columnNames = {"user_id", "archive_id"}),
        @UniqueConstraint(name = "uk_user_archive_item", columnNames = {"user_id", "archive_item_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_id")
    private Long favoriteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id")
    private ArchiveVibe archiveVibe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_item_id")
    private ArchiveItem archiveItem;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static Favorite ofVibe(User user, ArchiveVibe archiveVibe) {
        Favorite f = new Favorite();
        f.user = user;
        f.archiveVibe = archiveVibe;
        return f;
    }

    public static Favorite ofItem(User user, ArchiveItem archiveItem) {
        Favorite f = new Favorite();
        f.user = user;
        f.archiveItem = archiveItem;
        return f;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
