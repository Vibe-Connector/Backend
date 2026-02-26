package com.link.vibe.domain.item.repository;

import com.link.vibe.domain.item.entity.LightingDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LightingDetailRepository extends JpaRepository<LightingDetail, Long> {

    Optional<LightingDetail> findByItemId(Long itemId);
}
