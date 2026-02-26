package com.link.vibe.domain.item.integration;

import com.link.vibe.config.TestRedisConfig;
import com.link.vibe.config.TestS3Config;
import com.link.vibe.domain.item.entity.*;
import com.link.vibe.domain.item.repository.*;
import com.link.vibe.domain.language.entity.Language;
import com.link.vibe.domain.language.repository.LanguageRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestRedisConfig.class, TestS3Config.class})
@Transactional
class ItemIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager em;
    @Autowired LanguageRepository languageRepository;
    @Autowired ItemRepository itemRepository;
    @Autowired ItemCategoryRepository categoryRepository;
    @Autowired ItemTranslationRepository itemTranslationRepository;
    @Autowired ItemCategoryTranslationRepository categoryTranslationRepository;
    @Autowired MovieDetailRepository movieDetailRepository;
    @Autowired MusicDetailRepository musicDetailRepository;
    @Autowired LightingDetailRepository lightingDetailRepository;
    @Autowired CoffeeDetailRepository coffeeDetailRepository;

    private Language korean;
    private Language english;
    private ItemCategory movieCategory;
    private ItemCategory musicCategory;
    private ItemCategory lightingCategory;
    private ItemCategory coffeeCategory;

    @BeforeEach
    void setUp() {
        // 언어
        korean = createLanguage("ko", true);
        english = createLanguage("en", true);

        // 카테고리
        movieCategory = createCategory("movie", "[\"sight\"]");
        musicCategory = createCategory("music", "[\"hearing\"]");
        lightingCategory = createCategory("lighting", "[\"sight\"]");
        coffeeCategory = createCategory("coffee", "[\"taste\",\"smell\"]");

        em.flush();
        em.clear();
    }

    // ── Reflection 헬퍼 ──

    private void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            Field field = null;
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (field == null) throw new NoSuchFieldException(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private <T> T createEntity(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create entity: " + clazz.getSimpleName(), e);
        }
    }

    // ── 테스트 데이터 생성 ──

    private Language createLanguage(String code, boolean active) {
        Language lang = createEntity(Language.class);
        setField(lang, "languageCode", code);
        setField(lang, "isActive", active);
        em.persist(lang);
        return lang;
    }

    private ItemCategory createCategory(String key, String senseType) {
        ItemCategory cat = createEntity(ItemCategory.class);
        setField(cat, "categoryKey", key);
        setField(cat, "senseType", senseType);
        setField(cat, "isActive", true);
        em.persist(cat);
        return cat;
    }

    private Item createItem(ItemCategory category, String itemKey, String brand, boolean active) {
        Item item = createEntity(Item.class);
        setField(item, "category", category);
        setField(item, "itemKey", itemKey);
        setField(item, "brand", brand);
        setField(item, "imageUrl", "https://example.com/" + itemKey + ".jpg");
        setField(item, "externalLink", "https://example.com/" + itemKey);
        setField(item, "externalService", "test");
        setField(item, "isActive", active);
        em.persist(item);
        return item;
    }

    private void createItemTranslation(Item item, Language lang, String name, String desc) {
        ItemTranslation t = createEntity(ItemTranslation.class);
        setField(t, "item", item);
        setField(t, "language", lang);
        setField(t, "itemValue", name);
        setField(t, "description", desc);
        em.persist(t);
    }

    private void createCategoryTranslation(ItemCategory cat, Language lang, String name) {
        ItemCategoryTranslation t = createEntity(ItemCategoryTranslation.class);
        setField(t, "category", cat);
        setField(t, "language", lang);
        setField(t, "categoryValue", name);
        em.persist(t);
    }

    private MovieDetail createMovieDetail(Item item) {
        MovieDetail d = createEntity(MovieDetail.class);
        setField(d, "item", item);
        setField(d, "tmdbId", 12345);
        setField(d, "originalTitle", "Parasite");
        setField(d, "overview", "A gripping story");
        setField(d, "releaseDate", LocalDate.of(2019, 5, 30));
        setField(d, "runtime", 132);
        setField(d, "voteAverage", new BigDecimal("8.6"));
        setField(d, "voteCount", 15000);
        setField(d, "posterPath", "/poster.jpg");
        setField(d, "genres", "[\"드라마\",\"스릴러\"]");
        setField(d, "castInfo", "[{\"name\":\"송강호\",\"role\":\"기택\"}]");
        setField(d, "contentType", "movie");
        em.persist(d);
        return d;
    }

    private MusicDetail createMusicDetail(Item item) {
        MusicDetail d = createEntity(MusicDetail.class);
        setField(d, "item", item);
        setField(d, "artists", "[{\"name\":\"IU\",\"role\":\"vocalist\"}]");
        setField(d, "albumName", "Lilac");
        setField(d, "albumCoverUrl", "https://example.com/cover.jpg");
        setField(d, "trackDurationMs", 214000);
        setField(d, "releaseDate", LocalDate.of(2021, 3, 25));
        setField(d, "genres", "[\"K-Pop\",\"Ballad\"]");
        setField(d, "spotifyUri", "spotify:track:abc123");
        setField(d, "contentType", "track");
        em.persist(d);
        return d;
    }

    private LightingDetail createLightingDetail(Item item) {
        LightingDetail d = createEntity(LightingDetail.class);
        setField(d, "item", item);
        setField(d, "lightingKey", "warm_white_01");
        setField(d, "colorTempKelvin", 2700);
        setField(d, "colorTempName", "Warm White");
        setField(d, "brightnessPercent", 80);
        setField(d, "brightnessLevel", "밝음");
        setField(d, "lightingType", "smart_bulb");
        setField(d, "lightColor", "#FFD700");
        setField(d, "isDynamic", false);
        em.persist(d);
        return d;
    }

    private CoffeeDetail createCoffeeDetail(Item item) {
        CoffeeDetail d = createEntity(CoffeeDetail.class);
        setField(d, "item", item);
        setField(d, "capsuleKey", "arpeggio_01");
        setField(d, "capsuleName", "Arpeggio");
        setField(d, "line", "Original");
        setField(d, "intensity", 9);
        setField(d, "intensityMax", 13);
        setField(d, "cupSizes", "[{\"type\":\"Espresso\",\"ml\":40}]");
        setField(d, "origins", "[\"Colombia\",\"Brazil\"]");
        setField(d, "aromaProfile", "{\"primary\":[\"cocoa\",\"wood\"]}");
        setField(d, "body", 4);
        setField(d, "isDecaf", false);
        setField(d, "pricePerCapsuleKrw", 990);
        em.persist(d);
        return d;
    }

    // ═══════════════════════════════════════════
    // 영화 상세 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/items/{itemId}/movie")
    class GetMovieDetail {

        @Test
        @DisplayName("한국어 헤더로 정상 조회")
        void successKorean() throws Exception {
            Item item = createItem(movieCategory, "parasite", "CJ ENM", true);
            createMovieDetail(item);
            createItemTranslation(item, korean, "기생충", "블랙 코미디 걸작");
            createCategoryTranslation(movieCategory, korean, "영상");
            em.flush();
            em.clear();

            mockMvc.perform(get("/api/v1/items/{itemId}/movie", item.getItemId())
                            .header("Accept-Language", "ko"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.itemId").value(item.getItemId()))
                    .andExpect(jsonPath("$.data.itemName").value("기생충"))
                    .andExpect(jsonPath("$.data.description").value("블랙 코미디 걸작"))
                    .andExpect(jsonPath("$.data.categoryKey").value("movie"))
                    .andExpect(jsonPath("$.data.categoryName").value("영상"))
                    .andExpect(jsonPath("$.data.brand").value("CJ ENM"))
                    .andExpect(jsonPath("$.data.tmdbId").value(12345))
                    .andExpect(jsonPath("$.data.originalTitle").value("Parasite"))
                    .andExpect(jsonPath("$.data.runtime").value(132))
                    // JSON 파싱 결과(genres, castInfo)는 H2 JSON 컬럼 호환성 이슈로 단위 테스트에서 검증
                    .andExpect(jsonPath("$.data.contentType").value("movie"));
        }

        @Test
        @DisplayName("영어 헤더로 조회 — 영어 번역 반환")
        void successEnglish() throws Exception {
            Item item = createItem(movieCategory, "parasite", "CJ ENM", true);
            createMovieDetail(item);
            createItemTranslation(item, english, "Parasite", "Black comedy masterpiece");
            createCategoryTranslation(movieCategory, english, "Video");
            em.flush();
            em.clear();

            mockMvc.perform(get("/api/v1/items/{itemId}/movie", item.getItemId())
                            .header("Accept-Language", "en"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.itemName").value("Parasite"))
                    .andExpect(jsonPath("$.data.categoryName").value("Video"));
        }

        @Test
        @DisplayName("존재하지 않는 아이템 — 404")
        void notFound() throws Exception {
            mockMvc.perform(get("/api/v1/items/{itemId}/movie", 99999L)
                            .header("Accept-Language", "ko"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ITEM_001"));
        }

        @Test
        @DisplayName("비활성 아이템 — 404")
        void inactive() throws Exception {
            Item item = createItem(movieCategory, "inactive_movie", "Brand", false);
            createMovieDetail(item);
            em.flush();
            em.clear();

            mockMvc.perform(get("/api/v1/items/{itemId}/movie", item.getItemId())
                            .header("Accept-Language", "ko"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ITEM_001"));
        }

        @Test
        @DisplayName("카테고리 불일치 — 음악 아이템에 영화 상세 요청 → 404")
        void categoryMismatch() throws Exception {
            Item musicItem = createItem(musicCategory, "music_item", "Brand", true);
            createMusicDetail(musicItem);
            em.flush();
            em.clear();

            mockMvc.perform(get("/api/v1/items/{itemId}/movie", musicItem.getItemId())
                            .header("Accept-Language", "ko"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ITEM_001"));
        }

        @Test
        @DisplayName("번역 데이터 없으면 itemName/categoryName이 null")
        void noTranslation() throws Exception {
            Item item = createItem(movieCategory, "no_trans_movie", "Brand", true);
            createMovieDetail(item);
            em.flush();
            em.clear();

            mockMvc.perform(get("/api/v1/items/{itemId}/movie", item.getItemId())
                            .header("Accept-Language", "ko"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tmdbId").value(12345))
                    .andExpect(jsonPath("$.data.itemName").doesNotExist())
                    .andExpect(jsonPath("$.data.categoryName").doesNotExist());
        }
    }

    // ═══════════════════════════════════════════
    // 음악 상세 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/items/{itemId}/music")
    class GetMusicDetail {

        @Test
        @DisplayName("정상 조회")
        void success() throws Exception {
            Item item = createItem(musicCategory, "lilac", "Kakao Ent", true);
            createMusicDetail(item);
            createItemTranslation(item, korean, "라일락", "봄 감성 곡");
            createCategoryTranslation(musicCategory, korean, "음악");
            em.flush();
            em.clear();

            mockMvc.perform(get("/api/v1/items/{itemId}/music", item.getItemId())
                            .header("Accept-Language", "ko"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.itemName").value("라일락"))
                    .andExpect(jsonPath("$.data.categoryName").value("음악"))
                    .andExpect(jsonPath("$.data.albumName").value("Lilac"))
                    // JSON 파싱 결과(artists, genres)는 단위 테스트에서 검증
                    .andExpect(jsonPath("$.data.spotifyUri").value("spotify:track:abc123"));
        }
    }

    // ═══════════════════════════════════════════
    // 조명 상세 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/items/{itemId}/lighting")
    class GetLightingDetail {

        @Test
        @DisplayName("정상 조회")
        void success() throws Exception {
            Item item = createItem(lightingCategory, "warm_light", "Philips Hue", true);
            createLightingDetail(item);
            createItemTranslation(item, korean, "따뜻한 백색등", "아늑한 분위기 조명");
            createCategoryTranslation(lightingCategory, korean, "조명");
            em.flush();
            em.clear();

            mockMvc.perform(get("/api/v1/items/{itemId}/lighting", item.getItemId())
                            .header("Accept-Language", "ko"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.itemName").value("따뜻한 백색등"))
                    .andExpect(jsonPath("$.data.categoryName").value("조명"))
                    .andExpect(jsonPath("$.data.brand").value("Philips Hue"))
                    .andExpect(jsonPath("$.data.colorTempKelvin").value(2700))
                    .andExpect(jsonPath("$.data.colorTempName").value("Warm White"))
                    .andExpect(jsonPath("$.data.brightnessPercent").value(80))
                    .andExpect(jsonPath("$.data.lightingType").value("smart_bulb"))
                    .andExpect(jsonPath("$.data.isDynamic").value(false));
        }
    }

    // ═══════════════════════════════════════════
    // 커피 상세 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/items/{itemId}/coffee")
    class GetCoffeeDetail {

        @Test
        @DisplayName("정상 조회")
        void success() throws Exception {
            Item item = createItem(coffeeCategory, "arpeggio", "Nespresso", true);
            createCoffeeDetail(item);
            createItemTranslation(item, korean, "아르페지오", "진한 에스프레소 캡슐");
            createCategoryTranslation(coffeeCategory, korean, "커피");
            em.flush();
            em.clear();

            mockMvc.perform(get("/api/v1/items/{itemId}/coffee", item.getItemId())
                            .header("Accept-Language", "ko"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.itemName").value("아르페지오"))
                    .andExpect(jsonPath("$.data.categoryName").value("커피"))
                    .andExpect(jsonPath("$.data.brand").value("Nespresso"))
                    .andExpect(jsonPath("$.data.capsuleName").value("Arpeggio"))
                    .andExpect(jsonPath("$.data.line").value("Original"))
                    .andExpect(jsonPath("$.data.intensity").value(9))
                    // JSON 파싱 결과(cupSizes, origins, aromaProfile)는 단위 테스트에서 검증
                    .andExpect(jsonPath("$.data.pricePerCapsuleKrw").value(990))
                    .andExpect(jsonPath("$.data.isDecaf").value(false));
        }
    }
}
