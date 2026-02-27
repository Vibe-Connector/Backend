package com.link.vibe.domain.archive.service;

import com.link.vibe.domain.archive.dto.*;
import com.link.vibe.domain.archive.entity.ArchiveFolder;
import com.link.vibe.domain.archive.entity.ArchiveVibe;
import com.link.vibe.domain.archive.entity.Favorite;
import com.link.vibe.domain.archive.repository.ArchiveFolderRepository;
import com.link.vibe.domain.archive.repository.ArchiveVibeRepository;
import com.link.vibe.domain.archive.repository.FavoriteRepository;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ArchiveService {

    private final ArchiveVibeRepository archiveVibeRepository;
    private final ArchiveFolderRepository archiveFolderRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final VibeResultRepository vibeResultRepository;

    // ──── Vibe 아카이브 ────

    @Transactional
    public ArchiveVibeResponse archiveVibe(Long userId, ArchiveVibeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        VibeResult vibeResult = vibeResultRepository.findById(request.resultId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VIBE_RESULT_NOT_FOUND));

        if (archiveVibeRepository.existsByUserUserIdAndVibeResultResultId(userId, request.resultId())) {
            throw new BusinessException(ErrorCode.ARCHIVE_DUPLICATE);
        }

        ArchiveFolder folder = null;
        if (request.folderId() != null) {
            folder = archiveFolderRepository.findByFolderIdAndUserUserId(request.folderId(), userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ARCHIVE_FOLDER_NOT_FOUND));
            if (!folder.isVibeFolder()) {
                throw new BusinessException(ErrorCode.ARCHIVE_FOLDER_TYPE_MISMATCH);
            }
        }

        ArchiveVibe archiveVibe = ArchiveVibe.builder()
                .user(user)
                .vibeResult(vibeResult)
                .folder(folder)
                .memo(request.memo())
                .build();

        ArchiveVibe saved = archiveVibeRepository.save(archiveVibe);
        return ArchiveVibeResponse.of(saved, false);
    }

    public PageResponse<ArchiveVibeResponse> getArchiveVibes(
            Long userId, Long folderId, CursorPageRequest pageRequest) {

        int fetchSize = pageRequest.getFetchSize();
        PageRequest pageable = PageRequest.of(0, fetchSize);

        List<ArchiveVibe> archiveVibes;
        if (folderId != null) {
            archiveVibes = pageRequest.hasCursor()
                    ? archiveVibeRepository.findByUserAndFolderWithCursor(
                            userId, folderId, Long.parseLong(pageRequest.getCursor()), pageable)
                    : archiveVibeRepository.findByUserAndFolder(userId, folderId, pageable);
        } else {
            archiveVibes = pageRequest.hasCursor()
                    ? archiveVibeRepository.findByUserWithCursor(
                            userId, Long.parseLong(pageRequest.getCursor()), pageable)
                    : archiveVibeRepository.findByUser(userId, pageable);
        }

        List<ArchiveVibeResponse> content = archiveVibes.stream()
                .map(av -> ArchiveVibeResponse.of(av, false))
                .toList();

        return PageResponse.of(content, pageRequest.getEffectiveSize(),
                item -> String.valueOf(item.archiveId()));
    }

    @Transactional
    public void deleteArchiveVibe(Long userId, Long archiveId) {
        ArchiveVibe archiveVibe = archiveVibeRepository.findByArchiveIdAndUserUserId(archiveId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARCHIVE_NOT_FOUND));

        archiveVibeRepository.delete(archiveVibe);
    }

    // ──── Vibe 즐겨찾기 ────

    @Transactional
    public FavoriteResponse toggleVibeFavorite(Long userId, Long archiveId) {
        ArchiveVibe archiveVibe = archiveVibeRepository.findByArchiveIdAndUserUserId(archiveId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARCHIVE_NOT_FOUND));

        return favoriteRepository.findByUserUserIdAndArchiveVibeArchiveId(userId, archiveId)
                .map(favorite -> {
                    favoriteRepository.delete(favorite);
                    return new FavoriteResponse(false);
                })
                .orElseGet(() -> {
                    Favorite favorite = Favorite.ofVibe(archiveVibe.getUser(), archiveVibe);
                    favoriteRepository.save(favorite);
                    return new FavoriteResponse(true);
                });
    }

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
