package com.link.vibe.domain.item.repository;

import com.link.vibe.domain.item.entity.CoffeeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoffeeDetailRepository extends JpaRepository<CoffeeDetail, Long> {

    Optional<CoffeeDetail> findByItemId(Long itemId);
}
