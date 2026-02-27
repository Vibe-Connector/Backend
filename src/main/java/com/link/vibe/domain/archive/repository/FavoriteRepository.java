package com.link.vibe.domain.archive.repository;

import com.link.vibe.domain.archive.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // Vibe 즐겨찾기
    Optional<Favorite> findByUserUserIdAndArchiveVibeArchiveId(Long userId, Long archiveId);

    boolean existsByUserUserIdAndArchiveVibeArchiveId(Long userId, Long archiveId);

    // 아이템 즐겨찾기
    Optional<Favorite> findByUserUserIdAndArchiveItemArchiveItemId(Long userId, Long archiveItemId);

    boolean existsByUserUserIdAndArchiveItemArchiveItemId(Long userId, Long archiveItemId);
}
