package com.link.vibe.domain.vibe.listener;

import com.link.vibe.domain.vibe.service.VibeImageGenerationService;
import com.link.vibe.global.event.VibeCompleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class VibeImageGenerationListener {

    private final VibeImageGenerationService service;

    @Async("imageGenerationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVibeComplete(VibeCompleteEvent event) {
        log.info("========== [IMAGE-GEN] 이벤트 수신: sessionId={} ==========", event.sessionId());
        try {
            service.generateAndSaveImage(event.sessionId());
            log.info("========== [IMAGE-GEN] 완료: sessionId={} ==========", event.sessionId());
        } catch (Exception e) {
            log.error("========== [IMAGE-GEN] 실패: sessionId={} ==========", event.sessionId(), e);
        }
    }
}
