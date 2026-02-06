package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.TimeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeOptionRepository extends JpaRepository<TimeOption, Long> {
    List<TimeOption> findByIsActiveTrueOrderBySortOrder();
}
