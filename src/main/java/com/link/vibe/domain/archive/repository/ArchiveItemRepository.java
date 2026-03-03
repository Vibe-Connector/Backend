package com.link.vibe.domain.archive.repository;

import com.link.vibe.domain.archive.entity.ArchiveItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArchiveItemRepository extends JpaRepository<ArchiveItem, Long> {

    boolean existsByUserUserIdAndItemItemId(Long userId, Long itemId);

    Optional<ArchiveItem> findByArchiveItemIdAndUserUserId(Long archiveItemId, Long userId);

    @Query("SELECT ai FROM ArchiveItem ai " +
            "JOIN FETCH ai.item i " +
            "JOIN FETCH i.category " +
            "WHERE ai.user.userId = :userId " +
            "AND ai.archiveItemId < :cursor " +
            "ORDER BY ai.archiveItemId DESC")
    List<ArchiveItem> findByUserWithCursor(@Param("userId") Long userId,
                                           @Param("cursor") Long cursor,
                                           Pageable pageable);

    @Query("SELECT ai FROM ArchiveItem ai " +
            "JOIN FETCH ai.item i " +
            "JOIN FETCH i.category " +
            "WHERE ai.user.userId = :userId " +
            "ORDER BY ai.archiveItemId DESC")
    List<ArchiveItem> findByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT ai FROM ArchiveItem ai " +
            "JOIN FETCH ai.item i " +
            "JOIN FETCH i.category " +
            "WHERE ai.user.userId = :userId " +
            "AND ai.folder.folderId = :folderId " +
            "AND ai.archiveItemId < :cursor " +
            "ORDER BY ai.archiveItemId DESC")
    List<ArchiveItem> findByUserAndFolderWithCursor(@Param("userId") Long userId,
                                                     @Param("folderId") Long folderId,
                                                     @Param("cursor") Long cursor,
                                                     Pageable pageable);

    @Query("SELECT ai FROM ArchiveItem ai " +
            "JOIN FETCH ai.item i " +
            "JOIN FETCH i.category " +
            "WHERE ai.user.userId = :userId " +
            "AND ai.folder.folderId = :folderId " +
            "ORDER BY ai.archiveItemId DESC")
    List<ArchiveItem> findByUserAndFolder(@Param("userId") Long userId,
                                           @Param("folderId") Long folderId,
                                           Pageable pageable);

    long countByFolderFolderId(Long folderId);
}
