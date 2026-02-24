package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.CompanionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanionOptionRepository extends JpaRepository<CompanionOption, Long> {
    List<CompanionOption> findByIsActiveTrueOrderByCompanionId();
}
