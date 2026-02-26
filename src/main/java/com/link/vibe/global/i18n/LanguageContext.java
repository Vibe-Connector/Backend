package com.link.vibe.global.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class LanguageContext {

    private static final String LANGUAGE_ID_ATTRIBUTE = "languageId";

    private LanguageContext() {
    }

    public static Long getLanguageId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return (Long) request.getAttribute(LANGUAGE_ID_ATTRIBUTE);
    }

    public static void setLanguageId(HttpServletRequest request, Long languageId) {
        request.setAttribute(LANGUAGE_ID_ATTRIBUTE, languageId);
    }
}
