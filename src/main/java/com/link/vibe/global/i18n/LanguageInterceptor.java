package com.link.vibe.global.i18n;

import com.link.vibe.domain.language.entity.Language;
import com.link.vibe.domain.language.repository.LanguageRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LanguageInterceptor implements HandlerInterceptor {

    private static final String DEFAULT_LANGUAGE = "ko";

    private final LanguageRepository languageRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String langCode = resolveLanguageCode(request);

        Language language = languageRepository
                .findByLanguageCodeAndIsActiveTrue(langCode)
                .orElseGet(() -> languageRepository
                        .findByLanguageCodeAndIsActiveTrue(DEFAULT_LANGUAGE)
                        .orElse(null));

        if (language != null) {
            LanguageContext.setLanguageId(request, language.getLanguageId());
        }

        return true;
    }

    private String resolveLanguageCode(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        try {
            Locale locale = Locale.LanguageRange.parse(acceptLanguage).stream()
                    .findFirst()
                    .map(range -> Locale.forLanguageTag(range.getRange()))
                    .orElse(Locale.KOREAN);
            return locale.getLanguage();
        } catch (IllegalArgumentException e) {
            return DEFAULT_LANGUAGE;
        }
    }
}
