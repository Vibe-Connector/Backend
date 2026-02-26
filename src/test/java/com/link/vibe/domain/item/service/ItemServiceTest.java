package com.link.vibe.domain.item.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.domain.item.dto.CoffeeDetailResponse;
import com.link.vibe.domain.item.dto.LightingDetailResponse;
import com.link.vibe.domain.item.dto.MovieDetailResponse;
import com.link.vibe.domain.item.dto.MusicDetailResponse;
import com.link.vibe.domain.item.entity.*;
import com.link.vibe.domain.item.repository.*;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock ItemRepository itemRepository;
    @Mock ItemTranslationRepository itemTranslationRepository;
    @Mock ItemCategoryTranslationRepository categoryTranslationRepository;
    @Mock MovieDetailRepository movieDetailRepository;
    @Mock MusicDetailRepository musicDetailRepository;
    @Mock LightingDetailRepository lightingDetailRepository;
    @Mock CoffeeDetailRepository coffeeDetailRepository;

    ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(
                itemRepository,
                itemTranslationRepository,
                categoryTranslationRepository,
                movieDetailRepository,
                musicDetailRepository,
                lightingDetailRepository,
                coffeeDetailRepository,
                new ObjectMapper()
        );
    }

    /**
     * 응답 빌딩까지 필요한 전체 mock (정상 조회 케이스용)
     */
    private Item createFullMockItem(Long itemId, Long categoryId, String categoryKey) {
        ItemCategory category = mock(ItemCategory.class);
        when(category.getCategoryId()).thenReturn(categoryId);
        when(category.getCategoryKey()).thenReturn(categoryKey);

        Item item = mock(Item.class);
        when(item.getItemId()).thenReturn(itemId);
        when(item.getCategory()).thenReturn(category);
        when(item.getIsActive()).thenReturn(true);
        when(item.getBrand()).thenReturn("TestBrand");
        when(item.getImageUrl()).thenReturn("https://example.com/img.jpg");
        when(item.getExternalLink()).thenReturn("https://example.com");
        when(item.getExternalService()).thenReturn("tmdb");
        return item;
    }

    private ItemTranslation createMockTranslation(String itemValue, String description) {
        ItemTranslation t = mock(ItemTranslation.class);
        when(t.getItemValue()).thenReturn(itemValue);
        when(t.getDescription()).thenReturn(description);
        return t;
    }

    private ItemCategoryTranslation createMockCategoryTranslation(String categoryValue) {
        ItemCategoryTranslation t = mock(ItemCategoryTranslation.class);
        when(t.getCategoryValue()).thenReturn(categoryValue);
        return t;
    }

    // ═══════════════════════════════════════════
    // 공통 에러 케이스
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("공통 에러")
    class CommonErrors {

        @Test
        @DisplayName("존재하지 않는 아이템 조회 시 ITEM_NOT_FOUND")
        void itemNotFound() {
            given(itemRepository.findById(99999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getMovieDetail(99999L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
        }

        @Test
        @DisplayName("비활성 아이템 조회 시 ITEM_NOT_FOUND")
        void inactiveItem() {
            Item item = mock(Item.class);
            when(item.getIsActive()).thenReturn(false);
            given(itemRepository.findById(1L)).willReturn(Optional.of(item));

            assertThatThrownBy(() -> itemService.getMovieDetail(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
        }
    }

    // ═══════════════════════════════════════════
    // 영화 상세 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("getMovieDetail")
    class GetMovieDetail {

        @Test
        @DisplayName("정상 조회 — 공통 정보 + 영화 상세 + JSON 파싱")
        void success() {
            Item item = createFullMockItem(1L, 1L, "movie");
            given(itemRepository.findById(1L)).willReturn(Optional.of(item));

            MovieDetail detail = mock(MovieDetail.class);
            when(detail.getTmdbId()).thenReturn(12345);
            when(detail.getOriginalTitle()).thenReturn("Parasite");
            when(detail.getRuntime()).thenReturn(132);
            when(detail.getVoteAverage()).thenReturn(new BigDecimal("8.6"));
            when(detail.getVoteCount()).thenReturn(15000);
            when(detail.getGenres()).thenReturn("[\"드라마\",\"스릴러\"]");
            when(detail.getCastInfo()).thenReturn("[{\"name\":\"송강호\",\"role\":\"기택\"}]");
            when(detail.getContentType()).thenReturn("movie");
            given(movieDetailRepository.findByItemId(1L)).willReturn(Optional.of(detail));

            // mock 생성을 given() 호출 전에 분리 — Mockito 중첩 stubbing 방지
            ItemTranslation translation = createMockTranslation("기생충", "블랙 코미디");
            ItemCategoryTranslation catTranslation = createMockCategoryTranslation("영상");
            given(itemTranslationRepository.findByItemItemIdAndLanguageLanguageId(1L, 1L))
                    .willReturn(Optional.of(translation));
            given(categoryTranslationRepository.findByCategoryCategoryIdAndLanguageLanguageId(1L, 1L))
                    .willReturn(Optional.of(catTranslation));

            MovieDetailResponse result = itemService.getMovieDetail(1L, 1L);

            assertThat(result.itemId()).isEqualTo(1L);
            assertThat(result.itemName()).isEqualTo("기생충");
            assertThat(result.description()).isEqualTo("블랙 코미디");
            assertThat(result.categoryKey()).isEqualTo("movie");
            assertThat(result.categoryName()).isEqualTo("영상");
            assertThat(result.brand()).isEqualTo("TestBrand");
            assertThat(result.tmdbId()).isEqualTo(12345);
            assertThat(result.originalTitle()).isEqualTo("Parasite");
            assertThat(result.genres()).containsExactly("드라마", "스릴러");
            assertThat(result.castInfo()).hasSize(1);
            assertThat(result.castInfo().get(0).name()).isEqualTo("송강호");
        }

        @Test
        @DisplayName("상세 정보가 없으면 ITEM_NOT_FOUND")
        void detailNotFound() {
            Item item = mock(Item.class);
            when(item.getIsActive()).thenReturn(true);
            given(itemRepository.findById(1L)).willReturn(Optional.of(item));
            given(movieDetailRepository.findByItemId(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getMovieDetail(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
        }

        @Test
        @DisplayName("languageId가 null이면 번역 필드가 null")
        void nullLanguageId() {
            Item item = createFullMockItem(1L, 1L, "movie");
            given(itemRepository.findById(1L)).willReturn(Optional.of(item));

            MovieDetail detail = mock(MovieDetail.class);
            when(detail.getGenres()).thenReturn(null);
            when(detail.getCastInfo()).thenReturn(null);
            given(movieDetailRepository.findByItemId(1L)).willReturn(Optional.of(detail));

            MovieDetailResponse result = itemService.getMovieDetail(1L, null);

            assertThat(result.itemName()).isNull();
            assertThat(result.description()).isNull();
            assertThat(result.categoryName()).isNull();
            assertThat(result.genres()).isEmpty();
            assertThat(result.castInfo()).isEmpty();
        }

        @Test
        @DisplayName("잘못된 JSON이면 빈 리스트 반환")
        void invalidJson() {
            Item item = createFullMockItem(1L, 1L, "movie");
            given(itemRepository.findById(1L)).willReturn(Optional.of(item));

            MovieDetail detail = mock(MovieDetail.class);
            when(detail.getGenres()).thenReturn("{invalid json}");
            when(detail.getCastInfo()).thenReturn("{invalid}");
            given(movieDetailRepository.findByItemId(1L)).willReturn(Optional.of(detail));

            MovieDetailResponse result = itemService.getMovieDetail(1L, null);

            assertThat(result.genres()).isEmpty();
            assertThat(result.castInfo()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════
    // 음악 상세 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("getMusicDetail")
    class GetMusicDetail {

        @Test
        @DisplayName("정상 조회")
        void success() {
            Item item = createFullMockItem(16L, 2L, "music");
            given(itemRepository.findById(16L)).willReturn(Optional.of(item));

            MusicDetail detail = mock(MusicDetail.class);
            when(detail.getArtists()).thenReturn("[{\"name\":\"IU\",\"role\":\"vocalist\"}]");
            when(detail.getAlbumName()).thenReturn("Lilac");
            when(detail.getTrackDurationMs()).thenReturn(214000);
            when(detail.getGenres()).thenReturn("[\"K-Pop\"]");
            when(detail.getSpotifyUri()).thenReturn("spotify:track:abc123");
            when(detail.getContentType()).thenReturn("track");
            given(musicDetailRepository.findByItemId(16L)).willReturn(Optional.of(detail));

            ItemTranslation translation = createMockTranslation("라일락", "봄 감성 곡");
            ItemCategoryTranslation catTranslation = createMockCategoryTranslation("음악");
            given(itemTranslationRepository.findByItemItemIdAndLanguageLanguageId(16L, 1L))
                    .willReturn(Optional.of(translation));
            given(categoryTranslationRepository.findByCategoryCategoryIdAndLanguageLanguageId(2L, 1L))
                    .willReturn(Optional.of(catTranslation));

            MusicDetailResponse result = itemService.getMusicDetail(16L, 1L);

            assertThat(result.itemId()).isEqualTo(16L);
            assertThat(result.itemName()).isEqualTo("라일락");
            assertThat(result.artists()).hasSize(1);
            assertThat(result.artists().get(0).name()).isEqualTo("IU");
            assertThat(result.albumName()).isEqualTo("Lilac");
            assertThat(result.genres()).containsExactly("K-Pop");
            assertThat(result.spotifyUri()).isEqualTo("spotify:track:abc123");
        }

        @Test
        @DisplayName("상세 정보가 없으면 ITEM_NOT_FOUND")
        void detailNotFound() {
            Item item = mock(Item.class);
            when(item.getIsActive()).thenReturn(true);
            given(itemRepository.findById(16L)).willReturn(Optional.of(item));
            given(musicDetailRepository.findByItemId(16L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getMusicDetail(16L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ITEM_NOT_FOUND);
        }
    }

    // ═══════════════════════════════════════════
    // 조명 상세 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("getLightingDetail")
    class GetLightingDetail {

        @Test
        @DisplayName("정상 조회")
        void success() {
            Item item = createFullMockItem(31L, 3L, "lighting");
            given(itemRepository.findById(31L)).willReturn(Optional.of(item));

            LightingDetail detail = mock(LightingDetail.class);
            when(detail.getColorTempKelvin()).thenReturn(2700);
            when(detail.getColorTempName()).thenReturn("Warm White");
            when(detail.getBrightnessPercent()).thenReturn(80);
            when(detail.getBrightnessLevel()).thenReturn("밝음");
            when(detail.getLightingType()).thenReturn("smart_bulb");
            when(detail.getLightColor()).thenReturn("#FFD700");
            when(detail.getIsDynamic()).thenReturn(false);
            given(lightingDetailRepository.findByItemId(31L)).willReturn(Optional.of(detail));

            ItemTranslation translation = createMockTranslation("따뜻한 백색등", "아늑한 조명");
            ItemCategoryTranslation catTranslation = createMockCategoryTranslation("조명");
            given(itemTranslationRepository.findByItemItemIdAndLanguageLanguageId(31L, 1L))
                    .willReturn(Optional.of(translation));
            given(categoryTranslationRepository.findByCategoryCategoryIdAndLanguageLanguageId(3L, 1L))
                    .willReturn(Optional.of(catTranslation));

            LightingDetailResponse result = itemService.getLightingDetail(31L, 1L);

            assertThat(result.itemId()).isEqualTo(31L);
            assertThat(result.itemName()).isEqualTo("따뜻한 백색등");
            assertThat(result.colorTempKelvin()).isEqualTo(2700);
            assertThat(result.brightnessPercent()).isEqualTo(80);
            assertThat(result.lightingType()).isEqualTo("smart_bulb");
            assertThat(result.isDynamic()).isFalse();
        }
    }

    // ═══════════════════════════════════════════
    // 커피 상세 조회
    // ═══════════════════════════════════════════

    @Nested
    @DisplayName("getCoffeeDetail")
    class GetCoffeeDetail {

        @Test
        @DisplayName("정상 조회 — JSON 배열 + JSON 객체 파싱 포함")
        void success() {
            Item item = createFullMockItem(46L, 4L, "coffee");
            given(itemRepository.findById(46L)).willReturn(Optional.of(item));

            CoffeeDetail detail = mock(CoffeeDetail.class);
            when(detail.getCapsuleName()).thenReturn("Arpeggio");
            when(detail.getLine()).thenReturn("Original");
            when(detail.getIntensity()).thenReturn(9);
            when(detail.getIntensityMax()).thenReturn(13);
            when(detail.getCupSizes()).thenReturn("[{\"type\":\"Espresso\",\"ml\":40}]");
            when(detail.getOrigins()).thenReturn("[\"Colombia\",\"Brazil\"]");
            when(detail.getAromaProfile()).thenReturn("{\"primary\":[\"cocoa\",\"wood\"]}");
            when(detail.getBody()).thenReturn(4);
            when(detail.getIsDecaf()).thenReturn(false);
            when(detail.getPricePerCapsuleKrw()).thenReturn(990);
            given(coffeeDetailRepository.findByItemId(46L)).willReturn(Optional.of(detail));

            ItemTranslation translation = createMockTranslation("아르페지오", "진한 에스프레소");
            ItemCategoryTranslation catTranslation = createMockCategoryTranslation("커피");
            given(itemTranslationRepository.findByItemItemIdAndLanguageLanguageId(46L, 1L))
                    .willReturn(Optional.of(translation));
            given(categoryTranslationRepository.findByCategoryCategoryIdAndLanguageLanguageId(4L, 1L))
                    .willReturn(Optional.of(catTranslation));

            CoffeeDetailResponse result = itemService.getCoffeeDetail(46L, 1L);

            assertThat(result.itemId()).isEqualTo(46L);
            assertThat(result.itemName()).isEqualTo("아르페지오");
            assertThat(result.capsuleName()).isEqualTo("Arpeggio");
            assertThat(result.intensity()).isEqualTo(9);
            assertThat(result.cupSizes()).hasSize(1);
            assertThat(result.cupSizes().get(0).type()).isEqualTo("Espresso");
            assertThat(result.origins()).containsExactly("Colombia", "Brazil");
            assertThat(result.aromaProfile().primary()).containsExactly("cocoa", "wood");
            assertThat(result.pricePerCapsuleKrw()).isEqualTo(990);
        }

        @Test
        @DisplayName("aromaProfile JSON이 null이면 null 반환")
        void nullAromaProfile() {
            Item item = createFullMockItem(46L, 4L, "coffee");
            given(itemRepository.findById(46L)).willReturn(Optional.of(item));

            CoffeeDetail detail = mock(CoffeeDetail.class);
            when(detail.getCapsuleName()).thenReturn("Test");
            when(detail.getLine()).thenReturn("Original");
            when(detail.getCupSizes()).thenReturn("[]");
            when(detail.getOrigins()).thenReturn(null);
            when(detail.getAromaProfile()).thenReturn(null);
            given(coffeeDetailRepository.findByItemId(46L)).willReturn(Optional.of(detail));

            CoffeeDetailResponse result = itemService.getCoffeeDetail(46L, null);

            assertThat(result.aromaProfile()).isNull();
            assertThat(result.origins()).isEmpty();
        }
    }
}
