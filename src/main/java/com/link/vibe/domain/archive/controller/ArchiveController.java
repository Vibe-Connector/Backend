package com.link.vibe.domain.archive.controller;

import com.link.vibe.domain.archive.dto.*;
import com.link.vibe.domain.archive.service.ArchiveService;
import com.link.vibe.global.common.ApiResponse;
import com.link.vibe.global.common.CursorPageRequest;
import com.link.vibe.global.common.PageResponse;
import com.link.vibe.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Archive", description = "아카이브 API — Vibe 결과 및 아이템 저장, 폴더 관리, 즐겨찾기")
@RestController
@RequestMapping("/api/v1/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    // ──── Vibe 아카이브 (B-20 ~ B-22) ────

    @Operation(summary = "Vibe 결과 아카이브 저장", description = "Vibe 결과를 아카이브에 저장합니다. 선택적으로 VIBE 타입 폴더에 분류할 수 있습니다. 동일 사용자가 같은 결과를 중복 저장할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "아카이브 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Vibe 결과 또는 폴더를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "폴더 타입 불일치 (ARCHIVE_005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 저장된 아카이브 (ARCHIVE_003)")
    })
    @PostMapping("/vibes")
    public ApiResponse<ArchiveVibeResponse> archiveVibe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ArchiveVibeRequest request) {
        return ApiResponse.ok(archiveService.archiveVibe(userDetails.getUserId(), request));
    }

    @Operation(summary = "아카이브 Vibe 목록 조회", description = "사용자가 아카이브한 Vibe 결과 목록을 커서 기반 페이지네이션으로 조회합니다. folderId로 폴더별 필터링이 가능합니다.")
    @GetMapping("/vibes")
    public ApiResponse<PageResponse<ArchiveVibeResponse>> getArchiveVibes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "폴더 ID (미지정 시 전체 조회)", example = "1")
            @RequestParam(required = false) Long folderId,
            @ModelAttribute CursorPageRequest pageRequest) {
        return ApiResponse.ok(archiveService.getArchiveVibes(
                userDetails.getUserId(), folderId, pageRequest));
    }

    @Operation(summary = "아카이브 Vibe 삭제", description = "아카이브한 Vibe 결과를 삭제합니다. 연관된 즐겨찾기는 ON DELETE CASCADE로 자동 삭제됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "아카이브 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아카이브를 찾을 수 없음 (ARCHIVE_001)")
    })
    @DeleteMapping("/vibes/{archiveId}")
    public ApiResponse<Void> deleteArchiveVibe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "아카이브 ID", example = "1") @PathVariable Long archiveId) {
        archiveService.deleteArchiveVibe(userDetails.getUserId(), archiveId);
        return ApiResponse.ok(null);
    }

    // ──── 폴더 CRUD (B-26 ~ B-29) ────

    @Operation(summary = "폴더 생성", description = "아카이브 폴더를 생성합니다. folderType으로 VIBE(Vibe 결과용) 또는 ITEM(개별 아이템용)을 지정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "폴더 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 폴더 타입 (ARCHIVE_006)")
    })
    @PostMapping("/folders")
    public ApiResponse<FolderResponse> createFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody FolderCreateRequest request) {
        return ApiResponse.ok(archiveService.createFolder(userDetails.getUserId(), request));
    }

    @Operation(summary = "폴더 목록 조회", description = "사용자의 아카이브 폴더 목록을 조회합니다. folderType 파라미터로 VIBE/ITEM 필터링 가능합니다.")
    @GetMapping("/folders")
    public ApiResponse<List<FolderResponse>> getFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "폴더 타입 필터 (VIBE/ITEM, 미지정 시 전체)", example = "VIBE")
            @RequestParam(required = false) String folderType) {
        return ApiResponse.ok(archiveService.getFolders(userDetails.getUserId(), folderType));
    }

    @Operation(summary = "폴더 수정", description = "폴더명, 썸네일, 정렬 순서를 수정합니다. folderType은 변경할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "폴더 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "폴더를 찾을 수 없음 (ARCHIVE_004)")
    })
    @PutMapping("/folders/{folderId}")
    public ApiResponse<FolderResponse> updateFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "폴더 ID", example = "1") @PathVariable Long folderId,
            @Valid @RequestBody FolderUpdateRequest request) {
        return ApiResponse.ok(archiveService.updateFolder(userDetails.getUserId(), folderId, request));
    }

    @Operation(summary = "폴더 삭제", description = "폴더를 삭제합니다. 폴더 내 아카이브의 folder_id는 자동으로 NULL 처리됩니다 (ON DELETE SET NULL).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "폴더 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "폴더를 찾을 수 없음 (ARCHIVE_004)")
    })
    @DeleteMapping("/folders/{folderId}")
    public ApiResponse<Void> deleteFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "폴더 ID", example = "1") @PathVariable Long folderId) {
        archiveService.deleteFolder(userDetails.getUserId(), folderId);
        return ApiResponse.ok(null);
    }
}
