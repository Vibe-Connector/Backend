package com.link.vibe.domain.archive.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.config.TestRedisConfig;
import com.link.vibe.config.TestS3Config;
import com.link.vibe.domain.archive.dto.ArchiveItemRequest;
import com.link.vibe.domain.archive.dto.ArchiveVibeRequest;
import com.link.vibe.domain.archive.dto.FolderCreateRequest;
import com.link.vibe.domain.archive.dto.FolderUpdateRequest;
import com.link.vibe.domain.archive.entity.ArchiveFolder;
import com.link.vibe.domain.archive.entity.ArchiveItem;
import com.link.vibe.domain.archive.entity.Favorite;
import com.link.vibe.domain.archive.repository.ArchiveFolderRepository;
import com.link.vibe.domain.archive.repository.ArchiveItemRepository;
import com.link.vibe.domain.archive.repository.ArchiveVibeRepository;
import com.link.vibe.domain.archive.repository.FavoriteRepository;
import com.link.vibe.domain.item.entity.Item;
import com.link.vibe.domain.item.entity.ItemCategory;
import com.link.vibe.domain.user.entity.User;
import com.link.vibe.domain.user.repository.UserRepository;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
import com.link.vibe.global.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestRedisConfig.class, TestS3Config.class})
@Transactional
class ArchiveIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired EntityManager em;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Autowired UserRepository userRepository;
    @Autowired VibeSessionRepository vibeSessionRepository;
    @Autowired VibeResultRepository vibeResultRepository;
    @Autowired ArchiveVibeRepository archiveVibeRepository;
    @Autowired ArchiveItemRepository archiveItemRepository;
    @Autowired ArchiveFolderRepository archiveFolderRepository;
    @Autowired FavoriteRepository favoriteRepository;

    private User testUser;
    private VibeResult vibeResult1;
    private VibeResult vibeResult2;
    private Item item1;
    private Item item2;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("archive-test@example.com")
                .password("password123")
                .nickname("archiveuser")
                .name("Archive Test User")
                .build());

        accessToken = jwtTokenProvider.createAccessToken(
                testUser.getUserId(), testUser.getEmail());

        // VibeResult 생성
        VibeSession session1 = vibeSessionRepository.save(
                VibeSession.builder().userId(testUser.getUserId()).build());
        vibeResult1 = vibeResultRepository.save(VibeResult.builder()
                .vibeSession(session1)
                .phrase("따뜻한 오후의 감성")
                .aiAnalysis("차분하고 편안한 분위기")
                .aiModelVersion("gpt-4o-mini")
                .processingTimeMs(1200)
                .build());

        VibeSession session2 = vibeSessionRepository.save(
                VibeSession.builder().userId(testUser.getUserId()).build());
        vibeResult2 = vibeResultRepository.save(VibeResult.builder()
                .vibeSession(session2)
                .phrase("활기찬 아침")
                .aiAnalysis("에너지 넘치는 분위기")
                .aiModelVersion("gpt-4o-mini")
                .processingTimeMs(800)
                .build());

        // Item 생성 (native query로 직접 삽입 — H2에서 엔티티 빌더 없이 처리)
        ItemCategory category = createCategory("coffee");
        item1 = createItem(category, "item-key-1", "스타벅스", "https://img.example.com/1.jpg");
        item2 = createItem(category, "item-key-2", "블루보틀", "https://img.example.com/2.jpg");

        flushAndClear();
    }

    private ItemCategory createCategory(String categoryKey) {
        em.createNativeQuery("INSERT INTO item_categories (category_key, sense_type, is_active, created_at) " +
                        "VALUES (:key, :sense, true, NOW())")
                .setParameter("key", categoryKey)
                .setParameter("sense", "[\"taste\"]")
                .executeUpdate();
        return em.createQuery("SELECT c FROM ItemCategory c WHERE c.categoryKey = :key", ItemCategory.class)
                .setParameter("key", categoryKey)
                .getSingleResult();
    }

    private Item createItem(ItemCategory category, String itemKey, String brand, String imageUrl) {
        em.createNativeQuery("INSERT INTO items (category_id, item_key, brand, image_url, is_active, created_at, updated_at) " +
                        "VALUES (:catId, :key, :brand, :url, true, NOW(), NOW())")
                .setParameter("catId", category.getCategoryId())
                .setParameter("key", itemKey)
                .setParameter("brand", brand)
                .setParameter("url", imageUrl)
                .executeUpdate();
        return em.createQuery("SELECT i FROM Item i WHERE i.itemKey = :key", Item.class)
                .setParameter("key", itemKey)
                .getSingleResult();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private ArchiveFolder createVibeFolder(String name) {
        return archiveFolderRepository.save(ArchiveFolder.builder()
                .user(testUser)
                .folderName(name)
                .folderType("VIBE")
                .sortOrder(0)
                .build());
    }

    private ArchiveFolder createItemFolder(String name) {
        return archiveFolderRepository.save(ArchiveFolder.builder()
                .user(testUser)
                .folderName(name)
                .folderType("ITEM")
                .sortOrder(0)
                .build());
    }

    // ═══════════════════════════════════════════
    // 폴더 CRUD
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/archives/folders - 폴더 생성")
    class CreateFolder {

        @Test
        @DisplayName("VIBE 타입 폴더를 생성할 수 있다")
        void createVibeFolder() throws Exception {
            var request = new FolderCreateRequest("주말 감성", "VIBE", null, 1);

            mockMvc.perform(post("/api/v1/archives/folders")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.folderName").value("주말 감성"))
                    .andExpect(jsonPath("$.data.folderType").value("VIBE"));
        }

        @Test
        @DisplayName("ITEM 타입 폴더를 생성할 수 있다")
        void createItemFolder() throws Exception {
            var request = new FolderCreateRequest("나만의 커피", "ITEM", null, 0);

            mockMvc.perform(post("/api/v1/archives/folders")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.folderType").value("ITEM"));
        }

        @Test
        @DisplayName("유효하지 않은 폴더 타입이면 400 에러")
        void invalidFolderType() throws Exception {
            var request = new FolderCreateRequest("잘못된 타입", "INVALID", null, 0);

            mockMvc.perform(post("/api/v1/archives/folders")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_006"));
        }

        @Test
        @DisplayName("미인증 사용자는 폴더를 생성할 수 없다 (401)")
        void unauthorized() throws Exception {
            var request = new FolderCreateRequest("폴더", "VIBE", null, 0);

            mockMvc.perform(post("/api/v1/archives/folders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/archives/folders - 폴더 목록 조회")
    class GetFolders {

        @Test
        @DisplayName("전체 폴더 목록을 조회할 수 있다")
        void getAll() throws Exception {
            createVibeFolder("바이브 폴더");
            createItemFolder("아이템 폴더");
            flushAndClear();

            mockMvc.perform(get("/api/v1/archives/folders")
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("folderType으로 필터링할 수 있다")
        void filterByType() throws Exception {
            createVibeFolder("바이브1");
            createVibeFolder("바이브2");
            createItemFolder("아이템1");
            flushAndClear();

            mockMvc.perform(get("/api/v1/archives/folders")
                            .header("Authorization", bearer(accessToken))
                            .param("folderType", "VIBE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/archives/folders/{folderId} - 폴더 수정")
    class UpdateFolder {

        @Test
        @DisplayName("폴더명을 수정할 수 있다")
        void success() throws Exception {
            ArchiveFolder folder = createVibeFolder("원래 이름");
            flushAndClear();

            var request = new FolderUpdateRequest("수정된 이름", null, null);

            mockMvc.perform(put("/api/v1/archives/folders/{folderId}", folder.getFolderId())
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.folderName").value("수정된 이름"));
        }

        @Test
        @DisplayName("존재하지 않는 폴더 수정 시 404")
        void notFound() throws Exception {
            var request = new FolderUpdateRequest("이름", null, null);

            mockMvc.perform(put("/api/v1/archives/folders/{folderId}", 99999L)
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_004"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/archives/folders/{folderId} - 폴더 삭제")
    class DeleteFolder {

        @Test
        @DisplayName("폴더를 삭제할 수 있다")
        void success() throws Exception {
            ArchiveFolder folder = createVibeFolder("삭제할 폴더");
            Long folderId = folder.getFolderId();
            flushAndClear();

            mockMvc.perform(delete("/api/v1/archives/folders/{folderId}", folderId)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            flushAndClear();
            assertThat(archiveFolderRepository.findById(folderId)).isEmpty();
        }
    }

    // ═══════════════════════════════════════════
    // Vibe 아카이브 CRUD
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/archives/vibes - Vibe 아카이브 저장")
    class ArchiveVibe {

        @Test
        @DisplayName("Vibe 결과를 아카이브에 저장할 수 있다")
        void success() throws Exception {
            var request = new ArchiveVibeRequest(vibeResult1.getResultId(), null, "좋은 감성");

            mockMvc.perform(post("/api/v1/archives/vibes")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.resultId").value(vibeResult1.getResultId()))
                    .andExpect(jsonPath("$.data.phrase").value("따뜻한 오후의 감성"))
                    .andExpect(jsonPath("$.data.memo").value("좋은 감성"))
                    .andExpect(jsonPath("$.data.isFavorite").value(false));
        }

        @Test
        @DisplayName("VIBE 폴더를 지정하여 저장할 수 있다")
        void withFolder() throws Exception {
            ArchiveFolder folder = createVibeFolder("감성 모음");
            flushAndClear();

            var request = new ArchiveVibeRequest(vibeResult1.getResultId(), folder.getFolderId(), null);

            mockMvc.perform(post("/api/v1/archives/vibes")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.folderId").value(folder.getFolderId()))
                    .andExpect(jsonPath("$.data.folderName").value("감성 모음"));
        }

        @Test
        @DisplayName("ITEM 폴더를 지정하면 타입 불일치 에러 (400)")
        void folderTypeMismatch() throws Exception {
            ArchiveFolder itemFolder = createItemFolder("아이템 폴더");
            flushAndClear();

            var request = new ArchiveVibeRequest(vibeResult1.getResultId(), itemFolder.getFolderId(), null);

            mockMvc.perform(post("/api/v1/archives/vibes")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_005"));
        }

        @Test
        @DisplayName("동일 결과 중복 저장 시 409 에러")
        void duplicate() throws Exception {
            archiveVibeRepository.save(com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                    .user(testUser).vibeResult(vibeResult1).memo(null).build());
            flushAndClear();

            var request = new ArchiveVibeRequest(vibeResult1.getResultId(), null, null);

            mockMvc.perform(post("/api/v1/archives/vibes")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_003"));
        }

        @Test
        @DisplayName("미인증 사용자는 아카이브 저장 불가 (401)")
        void unauthorized() throws Exception {
            var request = new ArchiveVibeRequest(vibeResult1.getResultId(), null, null);

            mockMvc.perform(post("/api/v1/archives/vibes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/archives/vibes - Vibe 아카이브 목록 조회")
    class GetArchiveVibes {

        @Test
        @DisplayName("아카이브 목록을 조회할 수 있다")
        void success() throws Exception {
            archiveVibeRepository.save(com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                    .user(testUser).vibeResult(vibeResult1).memo("메모1").build());
            archiveVibeRepository.save(com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                    .user(testUser).vibeResult(vibeResult2).memo("메모2").build());
            flushAndClear();

            mockMvc.perform(get("/api/v1/archives/vibes")
                            .header("Authorization", bearer(accessToken))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("folderId로 폴더별 필터링이 동작한다")
        void filterByFolder() throws Exception {
            ArchiveFolder folder = createVibeFolder("감성 폴더");
            archiveVibeRepository.save(com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                    .user(testUser).vibeResult(vibeResult1).folder(folder).build());
            archiveVibeRepository.save(com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                    .user(testUser).vibeResult(vibeResult2).build());
            flushAndClear();

            mockMvc.perform(get("/api/v1/archives/vibes")
                            .header("Authorization", bearer(accessToken))
                            .param("folderId", String.valueOf(folder.getFolderId()))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].folderId").value(folder.getFolderId()));
        }

        @Test
        @DisplayName("커서 기반 페이지네이션이 동작한다")
        void cursorPagination() throws Exception {
            archiveVibeRepository.save(
                    com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                            .user(testUser).vibeResult(vibeResult1).build());
            com.link.vibe.domain.archive.entity.ArchiveVibe av2 = archiveVibeRepository.save(
                    com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                            .user(testUser).vibeResult(vibeResult2).build());
            flushAndClear();

            // size=1 → 최신 1개만
            mockMvc.perform(get("/api/v1/archives/vibes")
                            .header("Authorization", bearer(accessToken))
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(true));

            // cursor 이용 → 다음 페이지
            mockMvc.perform(get("/api/v1/archives/vibes")
                            .header("Authorization", bearer(accessToken))
                            .param("size", "1")
                            .param("cursor", String.valueOf(av2.getArchiveId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.hasNext").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/archives/vibes/{archiveId} - Vibe 아카이브 삭제")
    class DeleteArchiveVibe {

        @Test
        @DisplayName("아카이브를 삭제할 수 있다")
        void success() throws Exception {
            com.link.vibe.domain.archive.entity.ArchiveVibe av = archiveVibeRepository.save(
                    com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                            .user(testUser).vibeResult(vibeResult1).build());
            Long archiveId = av.getArchiveId();
            flushAndClear();

            mockMvc.perform(delete("/api/v1/archives/vibes/{archiveId}", archiveId)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            flushAndClear();
            assertThat(archiveVibeRepository.findById(archiveId)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 아카이브 삭제 시 404")
        void notFound() throws Exception {
            mockMvc.perform(delete("/api/v1/archives/vibes/{archiveId}", 99999L)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_001"));
        }
    }

    // ═══════════════════════════════════════════
    // Vibe 즐겨찾기
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/archives/vibes/{archiveId}/favorite - Vibe 즐겨찾기 토글")
    class ToggleVibeFavorite {

        @Test
        @DisplayName("즐겨찾기를 등록할 수 있다")
        void addFavorite() throws Exception {
            com.link.vibe.domain.archive.entity.ArchiveVibe av = archiveVibeRepository.save(
                    com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                            .user(testUser).vibeResult(vibeResult1).build());
            flushAndClear();

            mockMvc.perform(post("/api/v1/archives/vibes/{archiveId}/favorite", av.getArchiveId())
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorited").value(true));
        }

        @Test
        @DisplayName("즐겨찾기를 다시 요청하면 해제된다 (토글)")
        void toggleOff() throws Exception {
            com.link.vibe.domain.archive.entity.ArchiveVibe av = archiveVibeRepository.save(
                    com.link.vibe.domain.archive.entity.ArchiveVibe.builder()
                            .user(testUser).vibeResult(vibeResult1).build());
            favoriteRepository.save(Favorite.ofVibe(testUser, av));
            flushAndClear();

            mockMvc.perform(post("/api/v1/archives/vibes/{archiveId}/favorite", av.getArchiveId())
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorited").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 아카이브에 즐겨찾기 시 404")
        void notFound() throws Exception {
            mockMvc.perform(post("/api/v1/archives/vibes/{archiveId}/favorite", 99999L)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_001"));
        }
    }

    // ═══════════════════════════════════════════
    // 아이템 아카이브 CRUD
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/archives/items - 아이템 아카이브 저장")
    class ArchiveItemTest {

        @Test
        @DisplayName("아이템을 아카이브에 저장할 수 있다")
        void success() throws Exception {
            var request = new ArchiveItemRequest(item1.getItemId(), null, null, "맛있었다");

            mockMvc.perform(post("/api/v1/archives/items")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.itemId").value(item1.getItemId()))
                    .andExpect(jsonPath("$.data.brand").value("스타벅스"))
                    .andExpect(jsonPath("$.data.memo").value("맛있었다"))
                    .andExpect(jsonPath("$.data.isFavorite").value(false));
        }

        @Test
        @DisplayName("ITEM 폴더를 지정하여 저장할 수 있다")
        void withFolder() throws Exception {
            ArchiveFolder folder = createItemFolder("커피 리스트");
            flushAndClear();

            var request = new ArchiveItemRequest(item1.getItemId(), folder.getFolderId(), null, null);

            mockMvc.perform(post("/api/v1/archives/items")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.folderId").value(folder.getFolderId()))
                    .andExpect(jsonPath("$.data.folderName").value("커피 리스트"));
        }

        @Test
        @DisplayName("VIBE 폴더를 지정하면 타입 불일치 에러 (400)")
        void folderTypeMismatch() throws Exception {
            ArchiveFolder vibeFolder = createVibeFolder("바이브 폴더");
            flushAndClear();

            var request = new ArchiveItemRequest(item1.getItemId(), vibeFolder.getFolderId(), null, null);

            mockMvc.perform(post("/api/v1/archives/items")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_005"));
        }

        @Test
        @DisplayName("동일 아이템 중복 저장 시 409 에러")
        void duplicate() throws Exception {
            archiveItemRepository.save(ArchiveItem.builder()
                    .user(testUser).item(item1).build());
            flushAndClear();

            var request = new ArchiveItemRequest(item1.getItemId(), null, null, null);

            mockMvc.perform(post("/api/v1/archives/items")
                            .header("Authorization", bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_003"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/archives/items - 아이템 아카이브 목록 조회")
    class GetArchiveItems {

        @Test
        @DisplayName("아카이브 아이템 목록을 조회할 수 있다")
        void success() throws Exception {
            archiveItemRepository.save(ArchiveItem.builder()
                    .user(testUser).item(item1).memo("아이템1").build());
            archiveItemRepository.save(ArchiveItem.builder()
                    .user(testUser).item(item2).memo("아이템2").build());
            flushAndClear();

            mockMvc.perform(get("/api/v1/archives/items")
                            .header("Authorization", bearer(accessToken))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(2));
        }

        @Test
        @DisplayName("folderId로 폴더별 필터링이 동작한다")
        void filterByFolder() throws Exception {
            ArchiveFolder folder = createItemFolder("커피 폴더");
            archiveItemRepository.save(ArchiveItem.builder()
                    .user(testUser).item(item1).folder(folder).build());
            archiveItemRepository.save(ArchiveItem.builder()
                    .user(testUser).item(item2).build());
            flushAndClear();

            mockMvc.perform(get("/api/v1/archives/items")
                            .header("Authorization", bearer(accessToken))
                            .param("folderId", String.valueOf(folder.getFolderId()))
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/archives/items/{archiveItemId} - 아이템 아카이브 삭제")
    class DeleteArchiveItem {

        @Test
        @DisplayName("아카이브 아이템을 삭제할 수 있다")
        void success() throws Exception {
            ArchiveItem ai = archiveItemRepository.save(ArchiveItem.builder()
                    .user(testUser).item(item1).build());
            Long archiveItemId = ai.getArchiveItemId();
            flushAndClear();

            mockMvc.perform(delete("/api/v1/archives/items/{archiveItemId}", archiveItemId)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            flushAndClear();
            assertThat(archiveItemRepository.findById(archiveItemId)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 아카이브 아이템 삭제 시 404")
        void notFound() throws Exception {
            mockMvc.perform(delete("/api/v1/archives/items/{archiveItemId}", 99999L)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_002"));
        }
    }

    // ═══════════════════════════════════════════
    // 아이템 즐겨찾기
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/archives/items/{archiveItemId}/favorite - 아이템 즐겨찾기 토글")
    class ToggleItemFavorite {

        @Test
        @DisplayName("아이템 즐겨찾기를 등록할 수 있다")
        void addFavorite() throws Exception {
            ArchiveItem ai = archiveItemRepository.save(ArchiveItem.builder()
                    .user(testUser).item(item1).build());
            flushAndClear();

            mockMvc.perform(post("/api/v1/archives/items/{archiveItemId}/favorite", ai.getArchiveItemId())
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorited").value(true));
        }

        @Test
        @DisplayName("아이템 즐겨찾기를 다시 요청하면 해제된다 (토글)")
        void toggleOff() throws Exception {
            ArchiveItem ai = archiveItemRepository.save(ArchiveItem.builder()
                    .user(testUser).item(item1).build());
            favoriteRepository.save(Favorite.ofItem(testUser, ai));
            flushAndClear();

            mockMvc.perform(post("/api/v1/archives/items/{archiveItemId}/favorite", ai.getArchiveItemId())
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.favorited").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 아카이브 아이템에 즐겨찾기 시 404")
        void notFound() throws Exception {
            mockMvc.perform(post("/api/v1/archives/items/{archiveItemId}/favorite", 99999L)
                            .header("Authorization", bearer(accessToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ARCHIVE_002"));
        }
    }
}
