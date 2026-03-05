package com.link.vibe.domain.vibe.service;

import com.link.vibe.domain.vibe.dto.aiserver.AIServerIngestSessionRequest;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerRecommendRequest;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerRecommendResponse;
import com.link.vibe.domain.vibe.dto.aiserver.AIServerRecommendResponse.AIServerRecommendData;
import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class AIServerClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiPrefix;

    public AIServerClient(
            @Qualifier("aiServerRestTemplate") RestTemplate restTemplate,
            @Value("${aiserver.base-url}") String baseUrl,
            @Value("${aiserver.api-prefix}") String apiPrefix) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiPrefix = apiPrefix;
    }

    public AIServerRecommendData recommend(AIServerRecommendRequest request) {
        String url = baseUrl + apiPrefix + "/recommend/vibe";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        HttpEntity<AIServerRecommendRequest> entity = new HttpEntity<>(request, headers);

        log.info("AIServer 요청 URL: {}", url);
        log.info("AIServer 요청 body: {}", request);

        try {
            AIServerRecommendResponse response = restTemplate.postForObject(
                    url, entity, AIServerRecommendResponse.class);

            if (response == null || !response.success() || response.data() == null) {
                String msg = response != null ? response.message() : "null response";
                log.error("AIServer 추천 실패: {}", msg);
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "AIServer 추천 요청 실패: " + msg);
            }

            return response.data();
        } catch (HttpClientErrorException e) {
            log.error("AIServer 요청 오류 ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "AIServer 요청 오류 (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("AIServer 통신 오류: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "AIServer 연결 실패: " + e.getMessage());
        }
    }

    @Async("imageGenerationExecutor")
    public void ingestSession(AIServerIngestSessionRequest request) {
        String url = baseUrl + apiPrefix + "/graph/ingest/vibe-session";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AIServerIngestSessionRequest> entity = new HttpEntity<>(request, headers);

        try {
            restTemplate.postForObject(url, entity, String.class);
            log.info("AIServer 세션 학습 데이터 전송 완료: sessionId={}", request.sessionId());
        } catch (RestClientException e) {
            log.warn("AIServer 세션 학습 데이터 전송 실패 (비동기): sessionId={}, error={}",
                    request.sessionId(), e.getMessage());
        }
    }
}
