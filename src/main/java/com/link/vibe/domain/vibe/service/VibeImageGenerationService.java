package com.link.vibe.domain.vibe.service;

import com.link.vibe.domain.vibe.entity.VibeItem;
import com.link.vibe.domain.vibe.entity.VibePrompt;
import com.link.vibe.domain.vibe.entity.VibeResult;
import com.link.vibe.domain.vibe.entity.VibeSession;
import com.link.vibe.domain.vibe.repository.VibeItemRepository;
import com.link.vibe.domain.vibe.repository.VibeSessionRepository;
import com.link.vibe.global.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VibeImageGenerationService {

    private final VibeSessionRepository vibeSessionRepository;
    private final VibeItemRepository vibeItemRepository;
    private final OpenAiService openAiService;
    private final S3StorageService s3StorageService;

    @Transactional
    public void generateAndSaveImage(Long sessionId) {
        log.info("[IMAGE-GEN] Step 1: 세션 조회 시작 - sessionId={}", sessionId);

        // 1. 세션/결과/프롬프트 조회
        VibeSession session = vibeSessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다: " + sessionId));

        VibeResult result = session.getVibeResult();
        if (result == null) {
            log.info("[IMAGE-GEN] 결과가 없는 세션: sessionId={}", sessionId);
            return;
        }

        if (result.getGeneratedImageUrl() != null) {
            log.info("[IMAGE-GEN] 이미 이미지가 생성된 세션: sessionId={}", sessionId);
            return;
        }

        VibePrompt prompt = session.getVibePrompt();
        log.info("[IMAGE-GEN] Step 2: 아이템 조회 - resultId={}", result.getResultId());

        // 2. 카테고리별 유사도 최고 아이템 추출
        List<VibeItem> vibeItems = vibeItemRepository.findByResultIdWithItemDetails(result.getResultId());
        Map<String, String> topItems = vibeItems.stream()
                .collect(Collectors.groupingBy(
                        vi -> vi.getItem().getCategory().getCategoryKey(),
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(vi -> vi.getMatchScore() != null ? vi.getMatchScore() : BigDecimal.ZERO)),
                                opt -> opt.map(vi -> vi.getItem().getItemKey()).orElse("")
                        )
                ));

        // 3. 사용자 선택 옵션 추출
        List<String> moods = prompt != null && prompt.getFinalPrompt() != null
                ? extractMoodsFromPrompt(prompt.getFinalPrompt())
                : Collections.emptyList();

        String time = prompt != null && prompt.getTimeOption() != null
                ? prompt.getTimeOption().getTimeKey() : "afternoon";
        String weather = prompt != null && prompt.getWeatherOption() != null
                ? prompt.getWeatherOption().getWeatherKey() : "clear";
        String place = prompt != null && prompt.getPlaceOption() != null
                ? prompt.getPlaceOption().getPlaceKey() : "home";
        String companion = prompt != null && prompt.getCompanionOption() != null
                ? prompt.getCompanionOption().getCompanionKey() : "alone";

        // 4. DALL-E 프롬프트 구성
        String imagePrompt = openAiService.buildImagePrompt(
                result.getPhrase(), moods, time, weather, place, companion, topItems);

        log.info("[IMAGE-GEN] Step 3: DALL-E 호출 - promptLength={}", imagePrompt.length());

        // 5. DALL-E API 호출
        String tempImageUrl = openAiService.generateImage(imagePrompt);
        log.info("[IMAGE-GEN] Step 4: 이미지 URL 수신 - url={}", tempImageUrl);

        // 6. 임시 URL에서 이미지 다운로드 (Java HttpClient 사용)
        byte[] imageBytes = downloadImage(tempImageUrl);
        log.info("[IMAGE-GEN] Step 5: 이미지 다운로드 완료 - size={}bytes", imageBytes.length);

        // 7. S3 업로드
        String s3Url = s3StorageService.uploadBytes("vibe-images", imageBytes, "image/png", ".png");
        log.info("[IMAGE-GEN] Step 6: S3 업로드 완료 - s3Url={}", s3Url);

        // 8. DB 업데이트
        result.updateGeneratedImageUrl(s3Url);

        log.info("[IMAGE-GEN] 이미지 생성 전체 완료: sessionId={}", sessionId);
    }

    private byte[] downloadImage(String imageUrl) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new RuntimeException("이미지 다운로드 HTTP 오류: status=" + response.statusCode());
            }

            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new RuntimeException("이미지 다운로드 결과가 비어있습니다");
            }
            return body;
        } catch (Exception e) {
            throw new RuntimeException("이미지 다운로드 실패: " + e.getMessage(), e);
        }
    }

    private List<String> extractMoodsFromPrompt(String finalPrompt) {
        return finalPrompt.lines()
                .filter(line -> line.startsWith("기분:"))
                .findFirst()
                .map(line -> line.substring("기분:".length()).trim())
                .map(moods -> Arrays.asList(moods.split(",\\s*")))
                .orElse(Collections.emptyList());
    }
}
