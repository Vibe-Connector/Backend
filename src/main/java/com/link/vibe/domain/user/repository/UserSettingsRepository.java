package com.link.vibe.domain.user.repository;

import com.link.vibe.domain.user.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUserUserId(Long userId);
}
