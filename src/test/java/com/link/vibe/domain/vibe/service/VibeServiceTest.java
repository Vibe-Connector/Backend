package com.link.vibe.domain.vibe.service;

import com.link.vibe.domain.option.entity.*;
import com.link.vibe.domain.option.repository.*;
import com.link.vibe.domain.vibe.dto.VibeCreateRequest;
import com.link.vibe.domain.vibe.dto.VibeHistoryResponse;
import com.link.vibe.domain.vibe.dto.VibeResultResponse;
import com.link.vibe.domain.vibe.entity.VibeRequest;
import com.link.vibe.domain.vibe.entity.VibeRequestMood;
import com.link.vibe.domain.vibe.repository.VibeRequestRepository;
import com.link.vibe.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class VibeServiceTest {

    @InjectMocks
    private VibeService vibeService;

    @Mock
    private VibeRequestRepository vibeRequestRepository;
    @Mock
    private MoodKeywordRepository moodKeywordRepository;
    @Mock
    private TimeOptionRepository timeOptionRepository;
    @Mock
    private WeatherOptionRepository weatherOptionRepository;
    @Mock
    private PlaceOptionRepository placeOptionRepository;
    @Mock
    private CompanionOptionRepository companionOptionRepository;
    @Mock
    private OpenAiService openAiService;

    @Test
    @DisplayName("Vibe 생성 성공")
    void createVibe_success() {
        // given
        MoodKeyword mood1 = createMoodKeyword(1L, "languid", "나른한", "ADJECTIVE");
        MoodKeyword mood2 = createMoodKeyword(3L, "dreamy", "몽글몽글한", "ADJECTIVE");
        TimeOption time = createTimeOption(3L, "afternoon", "오후");
        WeatherOption weather = createWeatherOption(1L, "chilly", "쌀쌀한");
        PlaceOption place = createPlaceOption(1L, "cafe", "카페 창가");
        CompanionOption companion = createCompanionOption(1L, "alone", "혼자");

        given(moodKeywordRepository.findAllById(List.of(1L, 3L))).willReturn(List.of(mood1, mood2));
        given(timeOptionRepository.findById(3L)).willReturn(Optional.of(time));
        given(weatherOptionRepository.findById(1L)).willReturn(Optional.of(weather));
        given(placeOptionRepository.findById(1L)).willReturn(Optional.of(place));
        given(companionOptionRepository.findById(1L)).willReturn(Optional.of(companion));
        given(openAiService.generateVibe(anyList(), anyString(), anyString(), anyString(), anyString()))
                .willReturn(new OpenAiService.VibeResult(
                        "노곤한 오후, 카페 창가에 혼자 앉아 커피 향에 기대는 나른한 몽글몽글함",
                        "나른함과 몽글몽글함이라는 부드러운 감각이 어우러진 분위기입니다."
                ));
        given(openAiService.buildUserPrompt(anyList(), anyString(), anyString(), anyString(), anyString()))
                .willReturn("기분: 나른한, 몽글몽글한\n시간: 오후\n날씨: 쌀쌀한\n공간: 카페 창가\n동반자: 혼자\n");
        given(vibeRequestRepository.save(any(VibeRequest.class))).willAnswer(invocation -> {
            VibeRequest vr = invocation.getArgument(0);
            setField(vr, "vibeId", 1L);
            setField(vr, "createdAt", LocalDateTime.now());
            return vr;
        });

        VibeCreateRequest request = new VibeCreateRequest("test-session", List.of(1L, 3L), 3L, 1L, 1L, 1L);

        // when
        VibeResultResponse response = vibeService.createVibe(request);

        // then
        assertThat(response.vibeId()).isEqualTo(1L);
        assertThat(response.phrase()).contains("나른한");
        assertThat(response.selectedOptions().moods()).containsExactly("나른한", "몽글몽글한");
        assertThat(response.selectedOptions().time()).isEqualTo("오후");
        assertThat(response.selectedOptions().weather()).isEqualTo("쌀쌀한");
        assertThat(response.selectedOptions().place()).isEqualTo("카페 창가");
        assertThat(response.selectedOptions().companion()).isEqualTo("혼자");
    }

    @Test
    @DisplayName("존재하지 않는 옵션 ID로 Vibe 생성 시 예외")
    void createVibe_invalidOptionId_throwsException() {
        // given
        given(moodKeywordRepository.findAllById(List.of(999L))).willReturn(List.of());

        VibeCreateRequest request = new VibeCreateRequest("test-session", List.of(999L), 3L, 1L, 1L, 1L);

        // when & then
        assertThatThrownBy(() -> vibeService.createVibe(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 기분 키워드");
    }

    @Test
    @DisplayName("존재하지 않는 시간 옵션 ID로 Vibe 생성 시 예외")
    void createVibe_invalidTimeId_throwsException() {
        // given
        MoodKeyword mood = createMoodKeyword(1L, "languid", "나른한", "ADJECTIVE");
        given(moodKeywordRepository.findAllById(List.of(1L))).willReturn(List.of(mood));
        given(timeOptionRepository.findById(999L)).willReturn(Optional.empty());

        VibeCreateRequest request = new VibeCreateRequest("test-session", List.of(1L), 999L, 1L, 1L, 1L);

        // when & then
        assertThatThrownBy(() -> vibeService.createVibe(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 시간 옵션");
    }

    @Test
    @DisplayName("세션별 히스토리 조회")
    void getHistory_success() {
        // given
        MoodKeyword mood = createMoodKeyword(1L, "languid", "나른한", "ADJECTIVE");
        TimeOption time = createTimeOption(3L, "afternoon", "오후");
        WeatherOption weather = createWeatherOption(1L, "chilly", "쌀쌀한");
        PlaceOption place = createPlaceOption(1L, "cafe", "카페 창가");
        CompanionOption companion = createCompanionOption(1L, "alone", "혼자");

        VibeRequest vibeRequest = VibeRequest.builder()
                .sessionId("test-session")
                .timeOption(time)
                .weatherOption(weather)
                .placeOption(place)
                .companionOption(companion)
                .build();
        setField(vibeRequest, "vibeId", 1L);
        setField(vibeRequest, "createdAt", LocalDateTime.now());
        vibeRequest.applyResult("prompt", "분위기 문구", "분석", 1000);
        vibeRequest.addMood(new VibeRequestMood(mood));

        given(vibeRequestRepository.findBySessionIdWithOptions("test-session"))
                .willReturn(List.of(vibeRequest));

        // when
        List<VibeHistoryResponse> history = vibeService.getHistory("test-session");

        // then
        assertThat(history).hasSize(1);
        assertThat(history.get(0).vibeId()).isEqualTo(1L);
        assertThat(history.get(0).phrase()).isEqualTo("분위기 문구");
        assertThat(history.get(0).moods()).containsExactly("나른한");
        assertThat(history.get(0).time()).isEqualTo("오후");
    }

    @Test
    @DisplayName("Vibe 상세 조회 - 존재하지 않는 ID")
    void getVibeDetail_notFound() {
        // given
        given(vibeRequestRepository.findByIdWithOptions(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> vibeService.getVibeDetail(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    // --- helper methods ---

    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance: " + clazz.getSimpleName(), e);
        }
    }

    private MoodKeyword createMoodKeyword(Long id, String key, String text, String category) {
        MoodKeyword entity = createInstance(MoodKeyword.class);
        setField(entity, "keywordId", id);
        setField(entity, "keywordKey", key);
        setField(entity, "keywordText", text);
        setField(entity, "category", category);
        setField(entity, "isActive", true);
        return entity;
    }

    private TimeOption createTimeOption(Long id, String key, String text) {
        TimeOption entity = createInstance(TimeOption.class);
        setField(entity, "timeId", id);
        setField(entity, "timeKey", key);
        setField(entity, "timeText", text);
        setField(entity, "isActive", true);
        return entity;
    }

    private WeatherOption createWeatherOption(Long id, String key, String text) {
        WeatherOption entity = createInstance(WeatherOption.class);
        setField(entity, "weatherId", id);
        setField(entity, "weatherKey", key);
        setField(entity, "weatherText", text);
        setField(entity, "isActive", true);
        return entity;
    }

    private PlaceOption createPlaceOption(Long id, String key, String text) {
        PlaceOption entity = createInstance(PlaceOption.class);
        setField(entity, "placeId", id);
        setField(entity, "placeKey", key);
        setField(entity, "placeText", text);
        setField(entity, "isActive", true);
        return entity;
    }

    private CompanionOption createCompanionOption(Long id, String key, String text) {
        CompanionOption entity = createInstance(CompanionOption.class);
        setField(entity, "companionId", id);
        setField(entity, "companionKey", key);
        setField(entity, "companionText", text);
        setField(entity, "isActive", true);
        return entity;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
