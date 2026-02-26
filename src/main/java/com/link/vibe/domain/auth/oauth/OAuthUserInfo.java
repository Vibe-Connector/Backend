package com.link.vibe.domain.auth.oauth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthUserInfo {
    private final String providerUserId;
    private final String email;
    private final String name;
    private final String profileImageUrl;
    private final String accessToken;
    private final String refreshToken;
    private final Long expiresIn;
}
