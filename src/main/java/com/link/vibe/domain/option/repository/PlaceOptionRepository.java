package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.PlaceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceOptionRepository extends JpaRepository<PlaceOption, Long> {
    List<PlaceOption> findByIsActiveTrueOrderBySortOrder();
}
