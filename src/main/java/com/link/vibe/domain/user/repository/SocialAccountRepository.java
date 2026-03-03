package com.link.vibe.domain.user.repository;

import com.link.vibe.domain.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<SocialAccount> findByUserUserIdAndProvider(Long userId, String provider);

    List<SocialAccount> findAllByUserUserId(Long userId);
}
