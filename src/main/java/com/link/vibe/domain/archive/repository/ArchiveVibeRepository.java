package com.link.vibe.domain.archive.repository;

import com.link.vibe.domain.archive.entity.ArchiveVibe;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArchiveVibeRepository extends JpaRepository<ArchiveVibe, Long> {

    boolean existsByUserUserIdAndVibeResultResultId(Long userId, Long resultId);

    Optional<ArchiveVibe> findByArchiveIdAndUserUserId(Long archiveId, Long userId);

    @Query("SELECT av FROM ArchiveVibe av " +
            "JOIN FETCH av.vibeResult vr " +
            "JOIN FETCH vr.vibeSession vs " +
            "WHERE av.user.userId = :userId " +
            "AND av.archiveId < :cursor " +
            "ORDER BY av.archiveId DESC")
    List<ArchiveVibe> findByUserWithCursor(@Param("userId") Long userId,
                                           @Param("cursor") Long cursor,
                                           Pageable pageable);

    @Query("SELECT av FROM ArchiveVibe av " +
            "JOIN FETCH av.vibeResult vr " +
            "JOIN FETCH vr.vibeSession vs " +
            "WHERE av.user.userId = :userId " +
            "ORDER BY av.archiveId DESC")
    List<ArchiveVibe> findByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT av FROM ArchiveVibe av " +
            "JOIN FETCH av.vibeResult vr " +
            "JOIN FETCH vr.vibeSession vs " +
            "WHERE av.user.userId = :userId " +
            "AND av.folder.folderId = :folderId " +
            "AND av.archiveId < :cursor " +
            "ORDER BY av.archiveId DESC")
    List<ArchiveVibe> findByUserAndFolderWithCursor(@Param("userId") Long userId,
                                                     @Param("folderId") Long folderId,
                                                     @Param("cursor") Long cursor,
                                                     Pageable pageable);

    @Query("SELECT av FROM ArchiveVibe av " +
            "JOIN FETCH av.vibeResult vr " +
            "JOIN FETCH vr.vibeSession vs " +
            "WHERE av.user.userId = :userId " +
            "AND av.folder.folderId = :folderId " +
            "ORDER BY av.archiveId DESC")
    List<ArchiveVibe> findByUserAndFolder(@Param("userId") Long userId,
                                           @Param("folderId") Long folderId,
                                           Pageable pageable);

    long countByFolderFolderId(Long folderId);
}
