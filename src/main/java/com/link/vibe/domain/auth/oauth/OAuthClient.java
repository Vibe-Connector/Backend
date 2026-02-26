package com.link.vibe.domain.auth.oauth;

public interface OAuthClient {

    String getProvider();

    OAuthUserInfo getUserInfo(String authorizationCode, String redirectUri);
}
