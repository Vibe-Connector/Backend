package com.link.vibe.domain.item.repository;

import com.link.vibe.domain.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemVectorRepository extends JpaRepository<Item, Long> {

    @Query(value = """
            SELECT i.item_id, i.category_id, i.item_key, i.brand,
                   i.image_url, i.external_link, i.external_service, i.is_active,
                   1 - (i.embedding <=> CAST(:queryVector AS vector)) AS similarity_score
            FROM items i
            WHERE i.category_id = :categoryId
              AND i.is_active = true
              AND i.embedding IS NOT NULL
            ORDER BY i.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarItemsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    @Modifying
    @Query(value = "UPDATE items SET embedding = CAST(:embedding AS vector) WHERE item_id = :itemId",
            nativeQuery = true)
    void updateEmbedding(@Param("itemId") Long itemId, @Param("embedding") String embedding);

    @Query(value = "SELECT COUNT(*) FROM items WHERE embedding IS NOT NULL", nativeQuery = true)
    long countWithEmbedding();
}
