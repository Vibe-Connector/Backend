package com.link.vibe.global.logging;

import com.link.vibe.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    /**
     * 대상: com.link.vibe.domain 하위 모든 Service 클래스의 public 메서드
     */
    @Pointcut("execution(* com.link.vibe.domain..service.*.*(..))")
    private void serviceLayer() {}

    @Around("serviceLayer()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String args = summarizeArgs(joinPoint.getArgs());

        log.info("-> {}.{}({})", className, methodName, args);
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.info("<- {}.{} | {}ms | OK", className, methodName, duration);
            return result;
        } catch (BusinessException e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("x {}.{} | {}ms | {}: {}",
                    className, methodName, duration,
                    e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("x {}.{} | {}ms | {}",
                    className, methodName, duration, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 인자를 요약 문자열로 변환.
     * - null -> "null"
     * - 긴 문자열 -> 50자 잘라서 표시
     * - 민감 정보 (password, token) -> "***"
     */
    private String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        return Arrays.stream(args)
                .map(this::summarizeSingleArg)
                .collect(Collectors.joining(", "));
    }

    private String summarizeSingleArg(Object arg) {
        if (arg == null) return "null";
        String str = arg.toString();
        String lower = str.toLowerCase();
        if (lower.contains("password") || lower.contains("token") || lower.contains("secret")) {
            return "***";
        }
        if (str.length() > 50) {
            return str.substring(0, 50) + "...(" + str.length() + ")";
        }
        return str;
    }
}
