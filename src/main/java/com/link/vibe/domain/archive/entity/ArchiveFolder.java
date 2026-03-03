package com.link.vibe.domain.archive.entity;

import com.link.vibe.domain.user.entity.User;
import com.link.vibe.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "archive_folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchiveFolder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "folder_name", nullable = false, length = 100)
    private String folderName;

    @Column(name = "folder_type", nullable = false, length = 10)
    private String folderType;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder
    public ArchiveFolder(User user, String folderName, String folderType,
                         String thumbnailUrl, Integer sortOrder) {
        this.user = user;
        this.folderName = folderName;
        this.folderType = folderType;
        this.thumbnailUrl = thumbnailUrl;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public void update(String folderName, String thumbnailUrl, Integer sortOrder) {
        if (folderName != null) this.folderName = folderName;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (sortOrder != null) this.sortOrder = sortOrder;
    }

    public boolean isVibeFolder() {
        return "VIBE".equals(folderType);
    }

    public boolean isItemFolder() {
        return "ITEM".equals(folderType);
    }
}
