package com.link.vibe.domain.item.controller;

import com.link.vibe.domain.item.service.ItemEmbeddingBatchService;
import com.link.vibe.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Item Admin", description = "아이템 관리 API")
@RestController
@RequestMapping("/api/v1/admin/items")
@RequiredArgsConstructor
public class ItemAdminController {

    private final ItemEmbeddingBatchService itemEmbeddingBatchService;

    @Operation(
            summary = "아이템 임베딩 일괄 생성",
            description = """
                    모든 활성 아이템에 대해 OpenAI Embeddings API를 호출하여
                    pgvector 임베딩 벡터를 생성합니다.

                    **인증 필요:** Authorization 헤더에 Bearer Access Token을 포함해야 합니다.
                    """
    )
    @PostMapping("/embeddings/generate")
    public ApiResponse<Map<String, Object>> generateEmbeddings() {
        int count = itemEmbeddingBatchService.generateAllEmbeddings();
        return ApiResponse.ok(Map.of(
                "message", "임베딩 생성 완료",
                "successCount", count
        ));
    }
}
