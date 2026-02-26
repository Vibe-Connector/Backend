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
public class GoogleOAuthClient implements OAuthClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthClient(
            RestTemplate restTemplate,
            @Value("${oauth.google.client-id}") String clientId,
            @Value("${oauth.google.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String getProvider() {
        return "GOOGLE";
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String authorizationCode, String redirectUri) {
        // 1. 인가 코드 → 토큰 교환
        Map<String, Object> tokenResponse = exchangeToken(authorizationCode, redirectUri);
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

        Map<String, Object> userInfo = response.getBody();

        return OAuthUserInfo.builder()
                .providerUserId(String.valueOf(userInfo.get("id")))
                .email((String) userInfo.get("email"))
                .name((String) userInfo.get("name"))
                .profileImageUrl((String) userInfo.get("picture"))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn != null ? expiresIn.longValue() : null)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeToken(String authorizationCode, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", authorizationCode);
        params.add("redirect_uri", redirectUri);

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
            log.error("Google token exchange failed", e);
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }
}
