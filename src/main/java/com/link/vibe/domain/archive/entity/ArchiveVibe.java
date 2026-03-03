package com.link.vibe.domain.archive.entity;

import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.vibe.entity.VibeResult;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "archive_vibes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_result", columnNames = {"user_id", "result_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchiveVibe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "archive_id")
    private Long archiveId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private VibeResult vibeResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private ArchiveFolder folder;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public ArchiveVibe(User user, VibeResult vibeResult, ArchiveFolder folder, String memo) {
        this.user = user;
        this.vibeResult = vibeResult;
        this.folder = folder;
        this.memo = memo;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
