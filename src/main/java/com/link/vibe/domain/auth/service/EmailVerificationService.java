package com.link.vibe.domain.auth.service;

import com.link.vibe.global.exception.BusinessException;
import com.link.vibe.global.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String CODE_KEY_PREFIX = "email_verification:";
    private static final String VERIFIED_KEY_PREFIX = "email_verified:";
    private static final long VERIFIED_TTL_MINUTES = 10;

    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${email.verification.expiration}")
    private long codeExpiration;

    /**
     * 6자리 인증 코드를 생성하여 Redis에 저장하고 이메일로 발송합니다.
     */
    public void sendVerificationCode(String email) {
        String code = generateCode();
        String key = CODE_KEY_PREFIX + email;

        // Redis에 코드 저장 (기존 코드 덮어쓰기)
        redisTemplate.opsForValue().set(key, code, codeExpiration, TimeUnit.MILLISECONDS);

        // 인증 완료 상태 초기화 (재발송 시)
        redisTemplate.delete(VERIFIED_KEY_PREFIX + email);

        sendEmail(email, code);
    }

    /**
     * 인증 코드를 검증합니다. 일치하면 인증 완료 상태를 Redis에 저장합니다.
     */
    public boolean verifyCode(String email, String code) {
        String key = CODE_KEY_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        }

        if (!savedCode.equals(code)) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_MISMATCH);
        }

        // 인증 성공 → 인증 완료 상태 저장
        redisTemplate.opsForValue().set(
                VERIFIED_KEY_PREFIX + email, "true",
                VERIFIED_TTL_MINUTES, TimeUnit.MINUTES
        );

        // 사용된 인증 코드 삭제
        redisTemplate.delete(key);

        return true;
    }

    /**
     * 이메일이 인증 완료 상태인지 확인합니다.
     */
    public boolean isVerified(String email) {
        String key = VERIFIED_KEY_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String generateCode() {
        int code = secureRandom.nextInt(900000) + 100000; // 100000 ~ 999999
        return String.valueOf(code);
    }

    private void sendEmail(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[VibeLink] 이메일 인증 코드");
            helper.setText(buildEmailContent(code), true);

            mailSender.send(message);
            log.info("인증 코드 발송 완료: {}", to);
        } catch (MessagingException e) {
            log.error("인증 코드 발송 실패: {}", to, e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String buildEmailContent(String code) {
        return """
                <div style="font-family: 'Pretendard', sans-serif; max-width: 480px; margin: 0 auto; padding: 40px 20px;">
                    <h2 style="color: #171E03; margin-bottom: 24px;">VibeLink 이메일 인증</h2>
                    <p style="color: #333333; font-size: 16px; line-height: 1.6;">
                        아래 인증 코드를 입력해 주세요.
                    </p>
                    <div style="background: #F2F2F2; border-radius: 8px; padding: 20px; text-align: center; margin: 24px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #171E03;">%s</span>
                    </div>
                    <p style="color: #82898E; font-size: 14px;">
                        이 코드는 5분간 유효합니다.
                    </p>
                </div>
                """.formatted(code);
    }
}
