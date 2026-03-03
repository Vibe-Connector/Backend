package com.link.vibe.domain.archive.repository;

import com.link.vibe.domain.archive.entity.ArchiveFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArchiveFolderRepository extends JpaRepository<ArchiveFolder, Long> {

    List<ArchiveFolder> findByUserUserIdAndFolderTypeOrderBySortOrderAsc(
            Long userId, String folderType);

    List<ArchiveFolder> findByUserUserIdOrderBySortOrderAsc(Long userId);

    Optional<ArchiveFolder> findByFolderIdAndUserUserId(Long folderId, Long userId);

    long countByUserUserId(Long userId);
}
