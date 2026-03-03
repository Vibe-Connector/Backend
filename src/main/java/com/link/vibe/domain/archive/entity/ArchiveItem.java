package com.link.vibe.domain.archive.entity;

import com.link.vibe.domain.item.entity.Item;
import com.link.vibe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "archive_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_item", columnNames = {"user_id", "item_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchiveItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "archive_item_id")
    private Long archiveItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private ArchiveFolder folder;

    @Column(name = "reaction_id")
    private Long reactionId;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public ArchiveItem(User user, Item item, ArchiveFolder folder, Long reactionId, String memo) {
        this.user = user;
        this.item = item;
        this.folder = folder;
        this.reactionId = reactionId;
        this.memo = memo;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
