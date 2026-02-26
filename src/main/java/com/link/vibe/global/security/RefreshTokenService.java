package com.link.vibe.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh_token:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Refresh Token을 Redis에 저장합니다.
     * key: "refresh_token:{userId}", value: refreshToken
     */
    public void save(Long userId, String refreshToken) {
        String key = KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    /**
     * userId로 저장된 Refresh Token을 조회합니다.
     */
    public Optional<String> findByUserId(Long userId) {
        String key = KEY_PREFIX + userId;
        String token = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(token);
    }

    /**
     * 저장된 Refresh Token과 요청 토큰이 일치하는지 검증합니다.
     */
    public boolean validate(Long userId, String refreshToken) {
        return findByUserId(userId)
                .map(saved -> saved.equals(refreshToken))
                .orElse(false);
    }

    /**
     * Refresh Token을 삭제합니다 (로그아웃).
     */
    public void delete(Long userId) {
        String key = KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
