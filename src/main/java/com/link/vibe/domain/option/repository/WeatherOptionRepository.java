package com.link.vibe.domain.option.repository;

import com.link.vibe.domain.option.entity.WeatherOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeatherOptionRepository extends JpaRepository<WeatherOption, Long> {
    List<WeatherOption> findByIsActiveTrueOrderByWeatherId();
}
