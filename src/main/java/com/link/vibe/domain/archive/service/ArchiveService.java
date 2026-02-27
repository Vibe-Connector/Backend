package com.link.vibe.domain.archive.service;

import com.link.vibe.domain.archive.dto.*;
import com.link.vibe.domain.archive.entity.ArchiveFolder;
import com.link.vibe.domain.archive.repository.ArchiveFolderRepository;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ArchiveService {

    private final ArchiveFolderRepository archiveFolderRepository;
    private final UserRepository userRepository;

    // ──── 폴더 CRUD ────

    @Transactional
    public FolderResponse createFolder(Long userId, FolderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String folderType = request.folderType().toUpperCase();
        if (!"VIBE".equals(folderType) && !"ITEM".equals(folderType)) {
            throw new BusinessException(ErrorCode.ARCHIVE_INVALID_FOLDER_TYPE);
        }

        ArchiveFolder folder = ArchiveFolder.builder()
                .user(user)
                .folderName(request.folderName())
                .folderType(folderType)
                .thumbnailUrl(request.thumbnailUrl())
                .sortOrder(request.sortOrder())
                .build();

        ArchiveFolder saved = archiveFolderRepository.save(folder);
        return FolderResponse.of(saved, 0);
    }

    public List<FolderResponse> getFolders(Long userId, String folderType) {
        List<ArchiveFolder> folders;
        if (folderType != null && !folderType.isBlank()) {
            folders = archiveFolderRepository.findByUserUserIdAndFolderTypeOrderBySortOrderAsc(
                    userId, folderType.toUpperCase());
        } else {
            folders = archiveFolderRepository.findByUserUserIdOrderBySortOrderAsc(userId);
        }

        return folders.stream()
                .map(folder -> FolderResponse.of(folder, 0))
                .toList();
    }

    @Transactional
    public FolderResponse updateFolder(Long userId, Long folderId, FolderUpdateRequest request) {
        ArchiveFolder folder = archiveFolderRepository.findByFolderIdAndUserUserId(folderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARCHIVE_FOLDER_NOT_FOUND));

        folder.update(request.folderName(), request.thumbnailUrl(), request.sortOrder());

        return FolderResponse.of(folder, 0);
    }

    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        ArchiveFolder folder = archiveFolderRepository.findByFolderIdAndUserUserId(folderId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARCHIVE_FOLDER_NOT_FOUND));

        archiveFolderRepository.delete(folder);
    }
}
