package com.link.vibe.domain.item.repository;

import com.link.vibe.domain.item.entity.MusicDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MusicDetailRepository extends JpaRepository<MusicDetail, Long> {

    Optional<MusicDetail> findByItemId(Long itemId);
}
