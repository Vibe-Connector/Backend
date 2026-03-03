package com.link.vibe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 (COM)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COM_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COM_002", "잘못된 입력값입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "COM_003", "잘못된 타입입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COM_004", "지원하지 않는 HTTP 메서드입니다."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "COM_005", "잘못된 파라미터입니다."),

    // 인증/인가 (AUTH)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_002", "접근 권한이 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "AUTH_004", "비활성화된 계정입니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_005", "소셜 로그인에 실패했습니다."),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH_006", "지원하지 않는 소셜 로그인 제공자입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_007", "유효하지 않은 Refresh Token입니다."),

    // 사용자 (USER)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER_003", "이미 사용 중인 닉네임입니다."),

    // Vibe (VIBE)
    VIBE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "VIBE_001", "Vibe 세션을 찾을 수 없습니다."),
    VIBE_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "VIBE_002", "Vibe 결과를 찾을 수 없습니다."),
    VIBE_INVALID_KEYWORD(HttpStatus.BAD_REQUEST, "VIBE_003", "잘못된 키워드입니다."),

    // 아이템 (ITEM)
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM_001", "아이템을 찾을 수 없습니다."),

    // 피드 (FEED)
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "FEED_001", "피드를 찾을 수 없습니다."),
    FEED_ALREADY_EXISTS(HttpStatus.CONFLICT, "FEED_002", "이미 존재하는 피드입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "FEED_003", "댓글을 찾을 수 없습니다."),

    // 아카이브 (ARCHIVE)
    ARCHIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "ARCHIVE_001", "아카이브를 찾을 수 없습니다."),
    ARCHIVE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ARCHIVE_002", "아카이브 아이템을 찾을 수 없습니다."),
    ARCHIVE_DUPLICATE(HttpStatus.CONFLICT, "ARCHIVE_003", "이미 저장된 아카이브입니다."),
    ARCHIVE_FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ARCHIVE_004", "폴더를 찾을 수 없습니다."),
    ARCHIVE_FOLDER_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "ARCHIVE_005", "폴더 타입이 일치하지 않습니다."),
    ARCHIVE_INVALID_FOLDER_TYPE(HttpStatus.BAD_REQUEST, "ARCHIVE_006", "유효하지 않은 폴더 타입입니다."),

    // 알림 (NOTI)
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI_001", "알림을 찾을 수 없습니다."),

    // 파일 (FILE)
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_001", "파일 업로드에 실패했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE_002", "파일 크기가 제한(5MB)을 초과했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE_003", "지원하지 않는 파일 형식입니다. (JPEG, PNG, GIF, WebP만 허용)"),

    // 팔로우 (FOLLOW)
    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "FOLLOW_001", "이미 팔로우한 사용자입니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLLOW_002", "팔로우 관계를 찾을 수 없습니다."),
    FOLLOW_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FOLLOW_003", "자기 자신을 팔로우할 수 없습니다."),

    // 언어 (LANG)
    LANGUAGE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "LANG_001", "지원하지 않는 언어입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
