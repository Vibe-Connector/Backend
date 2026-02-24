package com.link.vibe.domain.vibe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.link.vibe.domain.option.entity.*;
import com.link.vibe.domain.option.repository.*;
import com.link.vibe.domain.vibe.dto.VibeCreateRequest;
import com.link.vibe.domain.vibe.dto.VibeHistoryResponse;
import com.link.vibe.domain.vibe.dto.VibeResultResponse;
import com.link.vibe.domain.vibe.entity.VibePrompt;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibePromptRepository;
import com.link.vibe.domain.vibe.repository.VibeResultRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
import com.link.vibe.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private VibeSessionRepository vibeSessionRepository;
    @Mock
    private VibePromptRepository vibePromptRepository;
    @Mock
    private VibeResultRepository vibeResultRepository;
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
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Vibe 생성 성공")
    void createVibe_success() {
        // given
        MoodKeyword mood1 = createMoodKeyword(1L, "languid", "감정");
        MoodKeyword mood2 = createMoodKeyword(3L, "dreamy", "분위기");
        TimeOption time = createTimeOption(3L, "afternoon");
        WeatherOption weather = createWeatherOption(1L, "chilly");
        PlaceOption place = createPlaceOption(1L, "cafe");
        CompanionOption companion = createCompanionOption(1L, "alone");

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
                .willReturn("기분: languid, dreamy\n시간: afternoon\n날씨: chilly\n공간: cafe\n동반자: alone\n");
        given(vibeSessionRepository.save(any(VibeSession.class))).willAnswer(invocation -> {
            VibeSession session = invocation.getArgument(0);
            setField(session, "sessionId", 1L);
            setField(session, "createdAt", LocalDateTime.now());
            return session;
        });
        given(vibePromptRepository.save(any(VibePrompt.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(vibeResultRepository.save(any(VibeResult.class))).willAnswer(invocation -> invocation.getArgument(0));

        VibeCreateRequest request = new VibeCreateRequest(List.of(1L, 3L), 3L, 1L, 1L, 1L);

        // when
        VibeResultResponse response = vibeService.createVibe(request);

        // then
        assertThat(response.sessionId()).isEqualTo(1L);
        assertThat(response.phrase()).contains("나른한");
        assertThat(response.selectedOptions().moods()).containsExactly("languid", "dreamy");
        assertThat(response.selectedOptions().time()).isEqualTo("afternoon");
        assertThat(response.selectedOptions().weather()).isEqualTo("chilly");
        assertThat(response.selectedOptions().place()).isEqualTo("cafe");
        assertThat(response.selectedOptions().companion()).isEqualTo("alone");
    }

    @Test
    @DisplayName("존재하지 않는 옵션 ID로 Vibe 생성 시 예외")
    void createVibe_invalidOptionId_throwsException() {
        // given
        given(moodKeywordRepository.findAllById(List.of(999L))).willReturn(List.of());

        VibeCreateRequest request = new VibeCreateRequest(List.of(999L), 3L, 1L, 1L, 1L);

        // when & then
        assertThatThrownBy(() -> vibeService.createVibe(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 기분 키워드");
    }

    @Test
    @DisplayName("존재하지 않는 시간 옵션 ID로 Vibe 생성 시 예외")
    void createVibe_invalidTimeId_throwsException() {
        // given
        MoodKeyword mood = createMoodKeyword(1L, "languid", "감정");
        given(moodKeywordRepository.findAllById(List.of(1L))).willReturn(List.of(mood));
        given(timeOptionRepository.findById(999L)).willReturn(Optional.empty());

        VibeCreateRequest request = new VibeCreateRequest(List.of(1L), 999L, 1L, 1L, 1L);

        // when & then
        assertThatThrownBy(() -> vibeService.createVibe(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 시간 옵션");
    }

    @Test
    @DisplayName("사용자별 히스토리 조회")
    void getHistory_success() {
        // given
        MoodKeyword mood = createMoodKeyword(1L, "languid", "감정");
        TimeOption time = createTimeOption(3L, "afternoon");
        WeatherOption weather = createWeatherOption(1L, "chilly");
        PlaceOption place = createPlaceOption(1L, "cafe");
        CompanionOption companion = createCompanionOption(1L, "alone");

        VibeSession session = createInstance(VibeSession.class);
        setField(session, "sessionId", 1L);
        setField(session, "userId", 1L);
        setField(session, "status", "COMPLETED");
        setField(session, "createdAt", LocalDateTime.now());

        VibePrompt prompt = createInstance(VibePrompt.class);
        setField(prompt, "vibeSession", session);
        setField(prompt, "moodKeywordIds", "[1]");
        setField(prompt, "timeOption", time);
        setField(prompt, "weatherOption", weather);
        setField(prompt, "placeOption", place);
        setField(prompt, "companionOption", companion);

        VibeResult result = createInstance(VibeResult.class);
        setField(result, "vibeSession", session);
        setField(result, "phrase", "분위기 문구");
        setField(result, "aiAnalysis", "분석");
        setField(result, "processingTimeMs", 1000);

        setField(session, "vibePrompt", prompt);
        setField(session, "vibeResult", result);

        given(vibeSessionRepository.findByUserIdWithDetails(1L)).willReturn(List.of(session));
        given(moodKeywordRepository.findAllById(List.of(1L))).willReturn(List.of(mood));

        // when
        List<VibeHistoryResponse> history = vibeService.getHistory(1L);

        // then
        assertThat(history).hasSize(1);
        assertThat(history.get(0).sessionId()).isEqualTo(1L);
        assertThat(history.get(0).phrase()).isEqualTo("분위기 문구");
        assertThat(history.get(0).moods()).containsExactly("languid");
        assertThat(history.get(0).time()).isEqualTo("afternoon");
    }

    @Test
    @DisplayName("Vibe 상세 조회 - 존재하지 않는 세션 ID")
    void getVibeDetail_notFound() {
        // given
        given(vibeSessionRepository.findByIdWithDetails(999L)).willReturn(Optional.empty());

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

    private MoodKeyword createMoodKeyword(Long id, String value, String category) {
        MoodKeyword entity = createInstance(MoodKeyword.class);
        setField(entity, "keywordId", id);
        setField(entity, "keywordValue", value);
        setField(entity, "category", category);
        return entity;
    }

    private TimeOption createTimeOption(Long id, String key) {
        TimeOption entity = createInstance(TimeOption.class);
        setField(entity, "timeId", id);
        setField(entity, "timeKey", key);
        setField(entity, "timeValue", LocalTime.of(14, 0));
        setField(entity, "period", "PM");
        setField(entity, "isActive", true);
        return entity;
    }

    private WeatherOption createWeatherOption(Long id, String key) {
        WeatherOption entity = createInstance(WeatherOption.class);
        setField(entity, "weatherId", id);
        setField(entity, "weatherKey", key);
        setField(entity, "isActive", true);
        return entity;
    }

    private PlaceOption createPlaceOption(Long id, String key) {
        PlaceOption entity = createInstance(PlaceOption.class);
        setField(entity, "placeId", id);
        setField(entity, "placeKey", key);
        setField(entity, "isActive", true);
        return entity;
    }

    private CompanionOption createCompanionOption(Long id, String key) {
        CompanionOption entity = createInstance(CompanionOption.class);
        setField(entity, "companionId", id);
        setField(entity, "companionKey", key);
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
