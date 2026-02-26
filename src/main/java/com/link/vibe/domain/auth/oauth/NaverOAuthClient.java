package com.link.vibe.domain.auth.oauth;

import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class NaverOAuthClient implements OAuthClient {

    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;

    public NaverOAuthClient(
            RestTemplate restTemplate,
            @Value("${oauth.naver.client-id}") String clientId,
            @Value("${oauth.naver.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getProvider() {
        return "NAVER";
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String authorizationCode, String redirectUri) {
        // 1. 인가 코드 → 토큰 교환
        Map<String, Object> tokenResponse = exchangeToken(authorizationCode);
        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        Number expiresIn = (Number) tokenResponse.get("expires_in");

        // 2. 사용자 정보 조회
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                USER_INFO_URL, HttpMethod.GET, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        Map<String, Object> body = response.getBody();
        Map<String, Object> naverResponse = (Map<String, Object>) body.get("response");

        if (naverResponse == null) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        return OAuthUserInfo.builder()
                .providerUserId(String.valueOf(naverResponse.get("id")))
                .email((String) naverResponse.get("email"))
                .name((String) naverResponse.get("name"))
                .profileImageUrl((String) naverResponse.get("profile_image"))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn != null ? expiresIn.longValue() : null)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
            }
            return response.getBody();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Naver token exchange failed", e);
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }
}
