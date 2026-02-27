-- ============================================================================
-- Vibe-Link Database Schema v3.2
-- AI 기반 공감각 큐레이션 플랫폼
-- 다국어 지원: 한국어(ko), 영어(en), 중국어(zh)
--
-- 버전: v3.2 (v3.1 기반 수정)
-- 작성일: 2025-02-12
--
-- ============================================================================
-- v3.1 → v3.2 변경사항 요약
-- ===========================f=================================================
--
-- [테이블 삭제]
--   user_terms_agreements — 약관 동의는 앱 레벨에서 처리
--   time_option_translations — 시간 옵션 번역 불필요 (timestamp + AM/PM 방식)
--   vibe_prompt_moods — mood 선택을 vibe_prompts.mood_keyword_ids JSON으로 통합
--
-- [테이블 신규]
--   favorites — archive_vibes 즐겨찾기 분리 (archive_vibes.is_favorite 대체)
--   comment_reactions — 댓글 좋아요 전용 테이블
--
-- [구조 변경]
--   languages: PK를 language_code → language_id(BIGINT)로 변경.
--              language_name, is_default 삭제. language_code는 UNIQUE로 유지.
--              → 모든 번역 테이블 FK가 language_code → language_id로 변경됨
--   users: phone 삭제. preferred_language → preferred_language_id FK 변경.
--   user_settings: theme 삭제 (LocalStorage에서 관리)
--   mood_keywords: keyword_key → keyword_value 컬럼명 변경.
--                  sort_order, is_active 삭제.
--   mood_keyword_translations: language_code → language_id, keyword_text → keyword_value
--   time_options: time_range_start/end → time_value(TIME) + period(AM/PM).
--                 sort_order 삭제.
--   weather_options: icon, sort_order 삭제
--   weather_option_translations: language_code → language_id, weather_text → weather_value
--   place_options: icon, sort_order 삭제
--   place_option_translations: language_code → language_id, place_text → place_value
--   companion_options: icon, sort_order 삭제
--   companion_option_translations: language_code → language_id, companion_text → companion_value
--   item_categories: icon 삭제. sense_type ENUM → JSON (복수 감각 지원)
--   item_category_translations: language_code → language_id, category_name → category_value
--   items: popularity_score 삭제
--   item_translations: language_code → language_id, item_name → item_value,
--                      recommendation_reason 삭제
--   music_details: artist_name/artist_mbid → artists JSON.
--                  lyrics TEXT 추가.
--   coffee_details: capsule_name_ko/en → capsule_name,
--                   flavor_notes_ko/en → flavor_notes (언어별 분리 삭제)
--   vibe_prompts: mood_keyword_ids JSON 추가 (vibe_prompt_moods 테이블 대체)
--   vibe_items: recommend_reason TEXT 추가. sort_order 삭제.
--   archive_folders: icon → thumbnail_url. color 삭제.
--   archive_vibes: is_favorite 삭제 → favorites 테이블로 분리
--   archive_items: reaction_id 추가 (feed_reactions 참조)
--
-- [비MVP 테이블 — 스키마 하단으로 분리]
--   reports, external_connections, search_histories,
--   trending_vibes, mood_trends, etl_job_logs (보류),
--   data_quality_checks (수정), lighting_products (보류)
--
-- [유지 (v3.1과 동일)]
--   파이프라인: [1단계] 수집 → 품질 검증 → PostgreSQL
--              [2단계] PostgreSQL → LLM(Graph RAG) → Neo4j
--   저장소: Neo4j, Redis 유지
--   삭제 유지: movie_mood_tags, music_mood_tags, lighting_mood_tags,
--             coffee_mood_tags, vibe_prompts.prompt_embedding
--
-- ============================================================================
-- 테이블 생성 순서 (FK 의존성 기준)
-- ============================================================================
--
-- [Level 0] FK 없음 — 가장 먼저 생성
--   languages, mood_keywords, time_options, weather_options,
--   place_options, companion_options, item_categories
--
-- [Level 1] Level 0 참조
--   users, mood_keyword_translations,
--   weather_option_translations, place_option_translations,
--   companion_option_translations, item_category_translations, items
--
-- [Level 2] Level 0~1 참조
--   social_accounts, user_settings, item_translations,
--   movie_details, music_details, lighting_details, coffee_details,
--   neo4j_sync_status, vibe_sessions, archive_folders,
--   follows, notifications
--
-- [Level 3] Level 0~2 참조
--   vibe_prompts, archive_items
--
-- [Level 4] Level 0~3 참조
--   vibe_results
--
-- [Level 5] Level 0~4 참조
--   vibe_items, archive_vibes, feeds
--
-- [Level 6] Level 0~5 참조
--   feed_reactions, feed_comments, favorites
--
-- [Level 7] Level 0~6 참조
--   comment_reactions
--
-- [비MVP / 보류]
--   reports, external_connections, search_histories,
--   trending_vibes, mood_trends, etl_job_logs,
--   data_quality_checks, lighting_products
--
-- ============================================================================


-- ============================================================================
-- LEVEL 0: 기반 테이블 (FK 의존성 없음)
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 지원 언어 마스터 테이블
-- 다국어 서비스를 위한 언어 정의. 레코드 추가만으로 신규 언어 확장 가능.
-- v3.2: PK를 language_code → language_id로 변경. language_name, is_default 삭제.
--       language_code는 ISO 639-1 기술 식별자로 UNIQUE 유지 (i18n 라이브러리 매핑용).
--       기본 언어 설정은 클라이언트 LocalStorage에서 사용자 로컬 정보 기반으로 처리.
-- ----------------------------------------------------------------------------
CREATE TABLE languages (
    language_id BIGINT PRIMARY KEY AUTO_INCREMENT,       -- 언어 고유 식별자
    language_code VARCHAR(5) UNIQUE NOT NULL,             -- 언어 코드 (ISO 639-1, 예: 'ko', 'en', 'zh')
    is_active BOOLEAN DEFAULT TRUE,                      -- 서비스 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP       -- 레코드 생성 시각
);


-- ----------------------------------------------------------------------------
-- 기분 키워드 마스터 테이블 (Vibe 입력 Step 1)
-- 사용자가 선택하는 기분/분위기 키워드 정의.
-- Neo4j의 MoodKeyword 노드와 1:1 매핑 (keyword_id로 동기화).
-- category는 LLM 태깅 카테고리(감정, 분위기, 에너지, 색감, 시청상황 등)에 대응.
-- v3.2: keyword_key → keyword_value 컬럼명 변경. sort_order, is_active 삭제.
--       VARCHAR(50)으로 키워드 자유 추가 가능.
-- ----------------------------------------------------------------------------
CREATE TABLE mood_keywords (
    keyword_id BIGINT PRIMARY KEY AUTO_INCREMENT,        -- 키워드 고유 식별자
    keyword_value VARCHAR(50) UNIQUE NOT NULL,            -- 키워드 값 (예: 'cozy', 'dreamy')
    category VARCHAR(50) NOT NULL,                        -- 태그 카테고리 (예: '감정', '분위기', '에너지', '색감')
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP        -- 레코드 생성 시각
);


-- ----------------------------------------------------------------------------
-- 시간대 옵션 테이블 (Vibe 입력 Step 2)
-- 사용자가 선택하는 시간대 옵션 정의.
-- v3.2: time_range_start/end → time_value + period 으로 단순화.
--       sort_order 삭제. time_option_translations 테이블 삭제됨.
-- ----------------------------------------------------------------------------
CREATE TABLE time_options (
    time_id BIGINT PRIMARY KEY AUTO_INCREMENT,            -- 시간대 옵션 고유 식별자
    time_key VARCHAR(50) UNIQUE NOT NULL,                 -- 시스템 내부 키 (예: 'morning', 'afternoon')
    time_value TIME NOT NULL,                             -- 지정 시간 (예: 09:00:00, 14:00:00)
    period ENUM('AM', 'PM') NOT NULL,                     -- 오전/오후 구분
    is_active BOOLEAN DEFAULT TRUE                        -- 활성화 여부
);


-- ----------------------------------------------------------------------------
-- 날씨 옵션 테이블 (Vibe 입력 Step 2)
-- 사용자가 선택하는 날씨 옵션 정의.
-- v3.2: icon, sort_order 삭제.
-- ----------------------------------------------------------------------------
CREATE TABLE weather_options (
    weather_id BIGINT PRIMARY KEY AUTO_INCREMENT,         -- 날씨 옵션 고유 식별자
    weather_key VARCHAR(50) UNIQUE NOT NULL,               -- 시스템 내부 키 (예: 'sunny', 'rainy')
    is_active BOOLEAN DEFAULT TRUE                        -- 활성화 여부
);


-- ----------------------------------------------------------------------------
-- 공간 옵션 테이블 (Vibe 입력 Step 3)
-- 사용자가 선택하는 장소/공간 옵션 정의.
-- v3.2: icon, sort_order 삭제.
-- ----------------------------------------------------------------------------
CREATE TABLE place_options (
    place_id BIGINT PRIMARY KEY AUTO_INCREMENT,           -- 공간 옵션 고유 식별자
    place_key VARCHAR(50) UNIQUE NOT NULL,                 -- 시스템 내부 키 (예: 'home', 'cafe')
    is_active BOOLEAN DEFAULT TRUE                        -- 활성화 여부
);


-- ----------------------------------------------------------------------------
-- 동반자 옵션 테이블 (Vibe 입력 Step 3)
-- 사용자가 선택하는 동반자 옵션 정의.
-- v3.2: icon, sort_order 삭제.
-- ----------------------------------------------------------------------------
CREATE TABLE companion_options (
    companion_id BIGINT PRIMARY KEY AUTO_INCREMENT,       -- 동반자 옵션 고유 식별자
    companion_key VARCHAR(50) UNIQUE NOT NULL,             -- 시스템 내부 키 (예: 'alone', 'with_partner')
    is_active BOOLEAN DEFAULT TRUE                        -- 활성화 여부
);


-- ----------------------------------------------------------------------------
-- 아이템 카테고리 마스터 테이블
-- 추천 아이템의 도메인 구분 (영상, 음악, 커피, 조명).
-- v3.2: icon 삭제. sense_type을 ENUM → JSON으로 변경 (하나의 카테고리가
--       복수 감각에 대응 가능, 예: 영상 = ["VISUAL", "AUDITORY"]).
-- ----------------------------------------------------------------------------
CREATE TABLE item_categories (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,        -- 카테고리 고유 식별자
    category_key VARCHAR(50) UNIQUE NOT NULL,              -- 시스템 내부 키 (예: 'video', 'music')
    sense_type JSON NOT NULL,                              -- 감각 유형 목록 (예: ["VISUAL", "AUDITORY"])
    is_active BOOLEAN DEFAULT TRUE,                        -- 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP         -- 레코드 생성 시각
);


-- ============================================================================
-- LEVEL 1: Level 0 테이블 참조
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 사용자 테이블
-- 회원 정보 및 인증 관련 기본 데이터.
-- v3.2: phone 삭제 (활용하지 않음).
--       preferred_language → preferred_language_id (FK 변경).
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,            -- 사용자 고유 식별자
    email VARCHAR(255) UNIQUE NOT NULL,                    -- 이메일 (로그인 ID)
    password VARCHAR(255),                                 -- 비밀번호 해시 (소셜 로그인 시 NULL)
    name VARCHAR(100),                                     -- 실명
    nickname VARCHAR(50) UNIQUE,                           -- 서비스 내 닉네임 (고유)
    gender ENUM('MALE', 'FEMALE', 'OTHER'),                -- 성별
    birth_year INT,                                        -- 출생 연도
    profile_image_url VARCHAR(500),                        -- 프로필 이미지 URL
    preferred_language_id BIGINT,                           -- 선호 언어 ID (FK → languages)
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED',
                'DELETED') DEFAULT 'ACTIVE',               -- 계정 상태
    last_login_at TIMESTAMP,                               -- 최근 로그인 시각
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 가입 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 정보 수정 시각
    deleted_at TIMESTAMP,                                  -- 탈퇴/삭제 시각 (소프트 삭제)
    FOREIGN KEY (preferred_language_id) REFERENCES languages(language_id)
);


-- ----------------------------------------------------------------------------
-- 기분 키워드 번역 테이블
-- mood_keywords의 다국어 번역 데이터.
-- v3.2: language_code → language_id, keyword_text → keyword_value
-- ----------------------------------------------------------------------------
CREATE TABLE mood_keyword_translations (
    translation_id BIGINT PRIMARY KEY AUTO_INCREMENT,     -- 번역 고유 식별자
    keyword_id BIGINT NOT NULL,                            -- 기분 키워드 ID (FK)
    language_id BIGINT NOT NULL,                           -- 언어 ID (FK)
    keyword_value VARCHAR(100) NOT NULL,                   -- 번역된 키워드 값 (예: '나른한', 'Drowsy')
    description TEXT,                                      -- 키워드 상세 설명
    UNIQUE KEY uk_keyword_lang (keyword_id, language_id),
    FOREIGN KEY (keyword_id) REFERENCES mood_keywords(keyword_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES languages(language_id)
);


-- ----------------------------------------------------------------------------
-- 날씨 옵션 번역 테이블
-- v3.2: language_code → language_id, weather_text → weather_value.
--       원문(한글)이 key, 번역이 value 역할.
-- ----------------------------------------------------------------------------
CREATE TABLE weather_option_translations (
    translation_id BIGINT PRIMARY KEY AUTO_INCREMENT,     -- 번역 고유 식별자
    weather_id BIGINT NOT NULL,                            -- 날씨 옵션 ID (FK)
    language_id BIGINT NOT NULL,                           -- 언어 ID (FK)
    weather_value VARCHAR(100) NOT NULL,                   -- 번역된 날씨 값 (예: '맑음', 'Sunny')
    UNIQUE KEY uk_weather_lang (weather_id, language_id),
    FOREIGN KEY (weather_id) REFERENCES weather_options(weather_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES languages(language_id)
);


-- ----------------------------------------------------------------------------
-- 공간 옵션 번역 테이블
-- v3.2: language_code → language_id, place_text → place_value
-- ----------------------------------------------------------------------------
CREATE TABLE place_option_translations (
    translation_id BIGINT PRIMARY KEY AUTO_INCREMENT,     -- 번역 고유 식별자
    place_id BIGINT NOT NULL,                              -- 공간 옵션 ID (FK)
    language_id BIGINT NOT NULL,                           -- 언어 ID (FK)
    place_value VARCHAR(100) NOT NULL,                     -- 번역된 공간 값 (예: '집', 'Home')
    UNIQUE KEY uk_place_lang (place_id, language_id),
    FOREIGN KEY (place_id) REFERENCES place_options(place_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES languages(language_id)
);


-- ----------------------------------------------------------------------------
-- 동반자 옵션 번역 테이블
-- v3.2: language_code → language_id, companion_text → companion_value
-- ----------------------------------------------------------------------------
CREATE TABLE companion_option_translations (
    translation_id BIGINT PRIMARY KEY AUTO_INCREMENT,     -- 번역 고유 식별자
    companion_id BIGINT NOT NULL,                          -- 동반자 옵션 ID (FK)
    language_id BIGINT NOT NULL,                           -- 언어 ID (FK)
    companion_value VARCHAR(100) NOT NULL,                 -- 번역된 동반자 값 (예: '혼자', 'Alone')
    UNIQUE KEY uk_companion_lang (companion_id, language_id),
    FOREIGN KEY (companion_id) REFERENCES companion_options(companion_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES languages(language_id)
);


-- ----------------------------------------------------------------------------
-- 아이템 카테고리 번역 테이블
-- v3.2: language_code → language_id, category_name → category_value
-- ----------------------------------------------------------------------------
CREATE TABLE item_category_translations (
    translation_id BIGINT PRIMARY KEY AUTO_INCREMENT,     -- 번역 고유 식별자
    category_id BIGINT NOT NULL,                           -- 카테고리 ID (FK)
    language_id BIGINT NOT NULL,                           -- 언어 ID (FK)
    category_value VARCHAR(100) NOT NULL,                  -- 번역된 카테고리 값 (예: '영상', 'Video')
    description TEXT,                                      -- 카테고리 설명
    UNIQUE KEY uk_category_lang (category_id, language_id),
    FOREIGN KEY (category_id) REFERENCES item_categories(category_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES languages(language_id)
);


-- ----------------------------------------------------------------------------
-- 공통 아이템 테이블
-- 모든 도메인(영상, 음악, 커피, 조명)의 공통 필드를 저장.
-- 도메인 고유 데이터는 확장 테이블({domain}_details)에 분리.
-- 1단계 파이프라인(수집 → PostgreSQL)의 저장 대상.
-- v3.2: popularity_score 삭제.
-- v3.1: mood_tags JSON 삭제 → Neo4j에서만 관리.
-- ----------------------------------------------------------------------------
CREATE TABLE items (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,            -- 아이템 고유 식별자
    category_id BIGINT NOT NULL,                           -- 아이템 카테고리 ID (FK → item_categories)
    item_key VARCHAR(100) UNIQUE NOT NULL,                 -- 시스템 내부 고유 키 (예: 'movie_tmdb_12345')
    brand VARCHAR(100),                                    -- 브랜드명 (예: 'Nespresso', NULL for movies)
    image_url VARCHAR(500),                                -- 대표 이미지 URL
    external_link VARCHAR(500),                            -- 외부 서비스 링크 (예: Netflix URL)
    external_service VARCHAR(50) DEFAULT 'OTHER',          -- 외부 서비스 식별자 (예: 'SPOTIFY', 'NETFLIX', 'NESPRESSO')
    is_active BOOLEAN DEFAULT TRUE,                        -- 활성화 여부 (비활성 시 추천에서 제외)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 레코드 수정 시각
    FOREIGN KEY (category_id) REFERENCES item_categories(category_id)
);


-- ============================================================================
-- LEVEL 2: Level 0~1 테이블 참조
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 소셜 로그인 계정 테이블
-- 사용자의 소셜 로그인(카카오, 구글, 네이버, 애플) 연동 정보.
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE social_accounts (
    social_id BIGINT PRIMARY KEY AUTO_INCREMENT,          -- 소셜 계정 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK → users)
    provider ENUM('KAKAO', 'GOOGLE', 'NAVER',
                  'APPLE') NOT NULL,                       -- 소셜 로그인 제공자
    provider_user_id VARCHAR(255) NOT NULL,                -- 제공자 측 사용자 ID
    access_token TEXT,                                     -- OAuth 액세스 토큰
    refresh_token TEXT,                                    -- OAuth 리프레시 토큰
    token_expires_at TIMESTAMP,                            -- 토큰 만료 시각
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 연동 시각
    UNIQUE KEY uk_provider_user (provider, provider_user_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 사용자 설정 테이블
-- 알림 설정 등 사용자 개인 환경 설정.
-- v3.2: theme 삭제 (LocalStorage에서 관리).
-- ----------------------------------------------------------------------------
CREATE TABLE user_settings (
    setting_id BIGINT PRIMARY KEY AUTO_INCREMENT,         -- 설정 고유 식별자
    user_id BIGINT UNIQUE NOT NULL,                        -- 사용자 ID (FK, 1:1 관계)
    push_enabled BOOLEAN DEFAULT TRUE,                     -- 푸시 알림 수신 여부
    email_notification BOOLEAN DEFAULT TRUE,               -- 이메일 알림 수신 여부
    default_share_privacy ENUM('PUBLIC', 'PRIVATE')
        DEFAULT 'PRIVATE',                                 -- 피드 공유 시 기본 공개 범위
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 레코드 수정 시각
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 아이템 번역 테이블
-- items의 다국어 번역 데이터 (아이템명, 설명).
-- v3.2: language_code → language_id, item_name → item_value,
--       recommendation_reason 삭제 (LLM 생성 결과에 포함).
-- ----------------------------------------------------------------------------
CREATE TABLE item_translations (
    translation_id BIGINT PRIMARY KEY AUTO_INCREMENT,     -- 번역 고유 식별자
    item_id BIGINT NOT NULL,                               -- 아이템 ID (FK)
    language_id BIGINT NOT NULL,                           -- 언어 ID (FK)
    item_value VARCHAR(255) NOT NULL,                      -- 번역된 아이템 값 (아이템명)
    description TEXT,                                      -- 번역된 설명
    UNIQUE KEY uk_item_lang (item_id, language_id),
    FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES languages(language_id)
);


-- ----------------------------------------------------------------------------
-- 영화/드라마 도메인 확장 테이블
-- items 테이블의 1:1 확장. TMDB API에서 수집한 영화/드라마 고유 데이터.
-- 1단계 파이프라인 저장 대상. 2단계에서 Neo4j Graph로 변환.
-- v3.2: 변동 없음.
-- v2.1 변경: 감독 + 출연진 구조화
-- 기존: cast_info JSON (비정형 배열)
-- 변경: {"director": {"name": "...", "tmdb_person_id": N},
--        "cast": [{"name": "...", "character": "...", "tmdb_person_id": N, "order": 0}, ...]}
-- v2.1 신규: TMDB 사용자 리뷰 (최대 10건)
-- [{"tmdb_review_id": "...", "author": "...", "rating": 8.0,
--   "content": "...", "created_at": "...", "language": "en"}, ...]
-- ----------------------------------------------------------------------------
CREATE TABLE movie_details (
    item_id BIGINT PRIMARY KEY,                            -- 아이템 ID (FK, PK — items와 1:1)
    tmdb_id INTEGER UNIQUE NOT NULL,                       -- TMDB 영화/드라마 ID
    original_title VARCHAR(500),                           -- 원제
    overview TEXT,                                         -- 줄거리 요약
    release_date DATE,                                     -- 개봉/공개일
    runtime INTEGER,                                       -- 상영시간 (분)
    vote_average DECIMAL(3,1),                             -- TMDB 평균 평점 (0.0~10.0)
    vote_count INTEGER,                                    -- TMDB 투표 수
    popularity DECIMAL(10,3),                              -- TMDB 인기도 점수
    poster_path VARCHAR(255),                              -- TMDB 포스터 이미지 경로
    genres JSON,                                           -- 장르 목록 (예: ["드라마", "SF"])
    keywords JSON,                                         -- TMDB 키워드 목록
    production_countries JSON,                             -- 제작 국가 목록
    original_language VARCHAR(10),                         -- 원작 언어 코드
    cast_info JSON,                                        -- 주요 출연진 정보
    release_dates JSONB,                                   -- 국가별 certification 통째로
    content_type VARCHAR(20) DEFAULT 'MOVIE',              -- 콘텐츠 유형 ('MOVIE', 'TV')
    tmdb_updated_at TIMESTAMP,                             -- TMDB 데이터 최종 갱신 시각
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 레코드 수정 시각
    FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------
-- 음악 도메인 확장 테이블
-- items 테이블의 1:1 확장. MusicBrainz/Last.fm/Deezer에서 수집한 음악 고유 데이터.
-- 1단계 파이프라인 저장 대상. 2단계에서 Neo4j Graph로 변환.
-- v3.2: artist_name/artist_mbid → artists JSON (피처링 등 복수 아티스트 지원).
--       artists 예시: [{"name":"IU","mbid":"xxx","role":"main"},
--                      {"name":"Suga","mbid":"yyy","role":"featuring"}]
--       mbid = MusicBrainz Artist ID (외부 API 아티스트 식별용).
--       lyrics TEXT 추가.
-- 한곡, 앨범명, 아티스트명, 아티스트 id, 발매일, 장르, 
-- ----------------------------------------------------------------------------
CREATE TABLE music_details (
    item_id BIGINT PRIMARY KEY,                            -- 아이템 ID (FK, PK — items와 1:1)
    musicbrainz_id VARCHAR(36) UNIQUE,                     -- MusicBrainz Recording MBID
    isrc VARCHAR(12),                                      -- 국제 표준 음반 코드 | ISO 3166-1 alpha-2로 가져옴(spotify)
    artists JSON NOT NULL,                                 -- 아티스트 목록 (JSON 배열, 피처링 포함) 
    album_name VARCHAR(500),                               -- 앨범명@
    album_cover_url VARCHAR(500),                          -- 앨범 커버 이미지 URL
    track_duration_ms INTEGER,                             -- 트랙 재생 시간 (밀리초)
    release_date DATE,                                     -- 발매일@
    genres JSON,                                           -- 장르 목록
    deezer_id INTEGER,                                     -- Deezer 트랙 ID
    preview_url VARCHAR(500),                              -- 미리듣기 URL (Deezer 30초)
    spotify_uri VARCHAR(100),                              -- Spotify URI (외부 연동용)
    lyrics TEXT,                                           -- 가사
    content_type VARCHAR(20) DEFAULT 'TRACK',              -- 콘텐츠 유형 ('TRACK', 'ALBUM', 'PLAYLIST')
    source_updated_at TIMESTAMP,                           -- 소스 데이터 최종 갱신 시각
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 레코드 수정 시각
    FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 조명 도메인 확장 테이블
-- items 테이블의 1:1 확장. 수동 문서 조사로 정의한 조명 속성 조합 데이터.
-- 수집 방식: 크롤링/API가 아닌 조명 설계 원리 기반 수동 정의 → CSV 로드.
-- color_temp_kelvin = 0은 자연광을 의미. RGB 컬러 조명은 color_temp_name에 색상명 기재.
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE lighting_details (
    item_id BIGINT PRIMARY KEY,                            -- 아이템 ID (FK, PK — items와 1:1)
    lighting_key VARCHAR(100) UNIQUE NOT NULL,              -- 조명 조합 고유 키 (예: 'warm_pendant_cozy')
    color_temp_kelvin INTEGER DEFAULT 0,                    -- 색온도 (Kelvin, 0 = 자연광)
    color_temp_name VARCHAR(100) NOT NULL,                  -- 색온도 범위명 (예: '전구색', '자연광', '블루')
    brightness_percent INTEGER NOT NULL
        CHECK (brightness_percent BETWEEN 0 AND 100),      -- 밝기 (0~100%)
    brightness_level VARCHAR(50),                           -- 밝기 수준명 (예: '은은한', '밝은')
    lighting_type VARCHAR(100) NOT NULL,                    -- 조명 방식 (예: '펜던트', '간접', '캔들')
    light_color VARCHAR(50) DEFAULT '웜화이트',              -- 조명 색상 (예: '웜화이트', '블루', '퍼플')
    position VARCHAR(100),                                  -- 조명 위치 (예: '천장', '벽면', '바닥')
    space_context VARCHAR(255),                             -- 어울리는 공간 (예: '카페, 거실, 서재')
    time_context VARCHAR(100),                              -- 어울리는 시간대 (예: '오후~저녁')
    is_dynamic BOOLEAN DEFAULT FALSE,                       -- 동적 조명 여부 (일출/일몰 시뮬레이션 등)
    dynamic_start_kelvin INTEGER,                           -- 동적 조명 시작 색온도
    dynamic_end_kelvin INTEGER,                             -- 동적 조명 종료 색온도
    linked_product_id BIGINT,                               -- 연결된 실제 제품 ID (추후 확장용, NULL 허용)
    hue_scene_id VARCHAR(100),                              -- Philips Hue 씬 ID (원클릭 연동용)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,         -- 레코드 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                        -- 레코드 수정 시각
    FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 커피(네스프레소 캡슐) 도메인 확장 테이블
-- items 테이블의 1:1 확장. 네스프레소 공식 데이터 기반 캡슐 상세 정보.
-- 1단계 파이프라인 저장 대상. 2단계에서 Neo4j Graph로 변환.
-- v3.2: capsule_name_ko/en → capsule_name (단일)으로 통합.
--       flavor_notes_ko/en → flavor_notes (단일)으로 통합.
--       언어별 이름/설명은 item_translations에서 관리.
-- coffee_details 스키마 v4.2 — 최종 확정
-- 네스프레소 공식 사이트 4개 캡슐 실데이터 검증 반영
-- 대상 라인: ORIGINAL, VERTUO (PROFESSIONAL 제외)
-- [수정] intensity_max CHECK: IN (12,14) → BETWEEN 1 AND 14 (라인 무관, 캡슐별 상이)
-- [수정] line CHECK: 'PROFESSIONAL' 제거
-- [수정] origin_countries → origins (국가 또는 지역명 모두 허용)
-- ============================================================================

CREATE TABLE coffee_details (
    item_id              BIGINT PRIMARY KEY,
    capsule_key          VARCHAR(100) UNIQUE NOT NULL,       -- 캡슐 고유 키 ('melozio', 'napoli')
    capsule_name         VARCHAR(255) NOT NULL,              -- 캡슐명 ('멜로지오', '나폴리')
    line                 VARCHAR(50) NOT NULL                -- 'ORIGINAL', 'VERTUO'
                         CHECK (line IN ('ORIGINAL', 'VERTUO')),
    -- ── 분류 ──
    sub_category         VARCHAR(100),                       -- '에스프레소', '룽고', '머그' 등
    -- ── 강도 ──
    intensity            INTEGER CHECK (intensity BETWEEN 1 AND 14),
    intensity_max        INTEGER CHECK (intensity_max BETWEEN 1 AND 14),  -- 캡슐별 최대 강도 (12 or 14, 라인 무관)
    -- ── 컵 사이즈 (복수 지원) ──
    cup_sizes            JSON NOT NULL,                      -- [{"type":"RISTRETTO","ml":25},{"type":"ESPRESSO","ml":40}]
    -- ── 원두 / 원산지 ──
    bean_type            VARCHAR(100),                       -- '100% Arabica' 등 (NULL 허용: 나폴리처럼 미명시 케이스)
    origins              JSON,                               -- ["브라질","온두라스"] 또는 ["라틴 아메리카"] (국가 또는 지역명)
    -- ── 로스팅 ──
    roast_level          VARCHAR(100),                       -- "DARK", "MEDIUM", "SPLIT_LIGHT_DARK" 등
    -- ── 아로마 / 풍미 ──
    aroma_profile        JSON,                               -- {"primary":["스파이시","우디","견과류"]} (1~3개 가변)
    flavor_notes         TEXT,                               -- 상세 풍미 설명 (사이트 원문)
    -- ── 수치 프로필 4축 (네스프레소 공식, 1~5) ──
    -- 스타벅스 콜라보 등 미제공 캡슐은 NULL
    body                 INTEGER CHECK (body BETWEEN 1 AND 5),
    bitterness           INTEGER CHECK (bitterness BETWEEN 1 AND 5),
    acidity              INTEGER CHECK (acidity BETWEEN 1 AND 5),
    roasting             INTEGER CHECK (roasting BETWEEN 1 AND 5),
    -- ── 기타 속성 ──
    is_decaf             BOOLEAN DEFAULT FALSE,
    is_limited_edition   BOOLEAN DEFAULT FALSE,              -- 시즌 한정 여부
    -- ── 가격 ──
    price_per_capsule_krw INTEGER,                           -- 캡슐 1개당 가격 (원)
    -- ── 타임스탬프 ──
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------
-- Neo4j 동기화 추적 테이블 (v3.0 신규) 
-- => 즉, pg 데이터가 n4j에 잘 들어갔는지 확인용 + 에러났을 경우 재처리 가능
-- 2단계 파이프라인에서 PostgreSQL 아이템이 Neo4j에 동기화되었는지 추적.
--
-- 사용 시나리오:
--   1. 1단계 완료 후 item이 PostgreSQL에 저장되면 sync_status = 'PENDING'
--   2. 2단계에서 LLM 전처리 + Neo4j 저장이 완료되면 'SYNCED'로 업데이트
--   3. 실패 시 'FAILED'로 마킹, error_message에 사유 기록
--   4. 재처리 필요 시 'PENDING'으로 리셋하여 2단계 재실행
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE neo4j_sync_status (
    sync_id BIGINT PRIMARY KEY AUTO_INCREMENT,            -- 동기화 추적 고유 식별자
    item_id BIGINT NOT NULL,                               -- 아이템 ID (FK → items)
    sync_status ENUM('PENDING', 'PROCESSING', 'SYNCED',
                     'FAILED') DEFAULT 'PENDING',          -- 동기화 상태
    neo4j_node_id VARCHAR(100),                            -- Neo4j 내부 노드 ID (참조/디버깅용)
    last_synced_at TIMESTAMP,                              -- 마지막 동기화 성공 시각
    retry_count INT DEFAULT 0,                             -- 재시도 횟수
    error_message TEXT,                                    -- 실패 시 에러 메시지
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 레코드 수정 시각
    UNIQUE KEY uk_item_sync (item_id),
    FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- Vibe 세션 테이블
-- 사용자의 Vibe 큐레이션 세션. 한 번의 Vibe 요청 = 한 개의 세션.
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE vibe_sessions (
    session_id BIGINT PRIMARY KEY AUTO_INCREMENT,         -- 세션 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK)
    status ENUM('IN_PROGRESS', 'COMPLETED',
                'CANCELLED') DEFAULT 'IN_PROGRESS',        -- 세션 진행 상태
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 세션 시작 시각
    completed_at TIMESTAMP,                                -- 세션 완료 시각
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 아카이브 폴더 테이블
-- 사용자가 저장한 Vibe 결과 또는 아이템을 정리하는 타입별 폴더.
-- v3.2: icon → thumbnail_url. color 삭제.
-- v3.2.1: folder_type 추가 — VIBE 폴더와 ITEM 폴더를 구분.
-- ----------------------------------------------------------------------------
CREATE TABLE archive_folders (
    folder_id BIGINT PRIMARY KEY AUTO_INCREMENT,          -- 폴더 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK)
    folder_name VARCHAR(100) NOT NULL,                     -- 폴더명
    folder_type ENUM('VIBE', 'ITEM') NOT NULL DEFAULT 'VIBE',  -- 폴더 타입 (VIBE: Vibe 결과용, ITEM: 개별 아이템용)
    thumbnail_url VARCHAR(500),                            -- 폴더 썸네일 URL (기본: 첫 저장 사진, 유저 업로드 가능)
    sort_order INT DEFAULT 0,                              -- 정렬 순서
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 수정 시각
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 팔로우 관계 테이블
-- 사용자 간 팔로우/팔로잉 관계.
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE follows (
    follow_id BIGINT PRIMARY KEY AUTO_INCREMENT,          -- 팔로우 관계 고유 식별자
    follower_id BIGINT NOT NULL,                           -- 팔로우하는 사용자 ID (FK)
    following_id BIGINT NOT NULL,                          -- 팔로우 받는 사용자 ID (FK)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 팔로우 시각
    UNIQUE KEY uk_follower_following (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 알림 테이블
-- 피드 반응, 댓글, 팔로우, 리포트 완료, 시스템 알림 등.
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,    -- 알림 고유 식별자
    user_id BIGINT NOT NULL,                               -- 수신 사용자 ID (FK)
    type ENUM('FEED_REACTION', 'FEED_COMMENT', 'FOLLOW',
              'REPORT_READY', 'SYSTEM') NOT NULL,          -- 알림 유형
    title VARCHAR(255) NOT NULL,                           -- 알림 제목
    body TEXT,                                             -- 알림 본문
    link_url VARCHAR(500),                                 -- 클릭 시 이동할 URL/딥링크
    reference_id BIGINT,                                   -- 참조 대상 ID (피드, 댓글 등 — 범용)
    is_read BOOLEAN DEFAULT FALSE,                         -- 읽음 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 알림 생성 시각
    read_at TIMESTAMP,                                     -- 읽은 시각
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ============================================================================
-- LEVEL 3: Level 0~2 테이블 참조
-- ============================================================================


-- ----------------------------------------------------------------------------
-- Vibe 프롬프트 테이블
-- 세션에서 사용자가 선택한 입력 조합 (기분 + 시간 + 날씨 + 공간 + 동반자).
-- final_prompt는 선택 조합을 LLM에 전달할 텍스트로 조합한 결과.
-- v3.2: mood_keyword_ids JSON 추가 (vibe_prompt_moods 테이블 대체).
--       복수 무드 키워드 선택 지원 (예: [1, 3, 7]).
-- v3.0: prompt_embedding 삭제 → Neo4j VibePrompt 노드에 저장.
-- ----------------------------------------------------------------------------
CREATE TABLE vibe_prompts (
    prompt_id BIGINT PRIMARY KEY AUTO_INCREMENT,          -- 프롬프트 고유 식별자
    session_id BIGINT UNIQUE NOT NULL,                     -- 세션 ID (FK, 1:1 관계)
    mood_keyword_ids JSON,                                 -- 선택한 무드 키워드 ID 목록 (예: [1, 3, 7] → mood_keywords.keyword_id 참조)
    time_id BIGINT,                                        -- 선택한 시간대 옵션 ID (FK, NULL 허용)
    weather_id BIGINT,                                     -- 선택한 날씨 옵션 ID (FK, NULL 허용)
    place_id BIGINT,                                       -- 선택한 공간 옵션 ID (FK, NULL 허용)
    companion_id BIGINT,                                   -- 선택한 동반자 옵션 ID (FK, NULL 허용)
    final_prompt TEXT,                                     -- LLM에 전달할 최종 프롬프트 텍스트
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 생성 시각
    FOREIGN KEY (session_id) REFERENCES vibe_sessions(session_id) ON DELETE CASCADE,
    FOREIGN KEY (time_id) REFERENCES time_options(time_id),
    FOREIGN KEY (weather_id) REFERENCES weather_options(weather_id),
    FOREIGN KEY (place_id) REFERENCES place_options(place_id),
    FOREIGN KEY (companion_id) REFERENCES companion_options(companion_id)
);


-- ----------------------------------------------------------------------------
-- 아카이브 아이템 테이블
-- 사용자가 개별 아이템을 저장한 이력.
-- v3.2: reaction_id 추가 (feed_reactions.reaction_id 논리적 참조).
-- v3.2.1: folder_id 추가 — ITEM 타입 폴더로 분류 가능.
-- ----------------------------------------------------------------------------
CREATE TABLE archive_items (
    archive_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,    -- 아카이브 아이템 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK)
    item_id BIGINT NOT NULL,                               -- 아이템 ID (FK)
    folder_id BIGINT,                                      -- 폴더 ID (FK, NULL = 미분류, ITEM 타입 폴더만)
    reaction_id BIGINT,                                    -- 피드 반응 ID (feed_reactions 참조, NULL 허용)
    memo VARCHAR(500),                                     -- 사용자 메모
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 저장 시각
    UNIQUE KEY uk_user_item (user_id, item_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES archive_folders(folder_id) ON DELETE SET NULL
);


-- ============================================================================
-- LEVEL 4: Level 0~3 테이블 참조
-- ============================================================================


-- ----------------------------------------------------------------------------
-- Vibe 결과 테이블
-- AI가 생성한 Vibe 큐레이션 결과 (이미지, 무드 문장, AI 분석).
-- 결과 수준의 무드 표현은 이 테이블의 phrase/ai_analysis에서 문장으로 제공.
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE vibe_results (
    result_id BIGINT PRIMARY KEY AUTO_INCREMENT,          -- 결과 고유 식별자
    session_id BIGINT UNIQUE NOT NULL,                     -- 세션 ID (FK, 1:1 관계)
    generated_image_url VARCHAR(500),                      -- AI 생성 이미지 URL
    phrase TEXT,                                           -- 무드 표현 문장 (예: '비 오는 오후, 따뜻한 코코아 한 잔의 여유')
    ai_analysis TEXT,                                      -- AI 분석 결과 (상세 큐레이션 설명)
    ai_model_version VARCHAR(50),                          -- 사용된 AI 모델 버전
    processing_time_ms INT,                                -- 처리 소요 시간 (밀리초)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 결과 생성 시각
    FOREIGN KEY (session_id) REFERENCES vibe_sessions(session_id) ON DELETE CASCADE
);


-- ============================================================================
-- LEVEL 5: Level 0~4 테이블 참조
-- ============================================================================


-- ----------------------------------------------------------------------------
-- Vibe 결과 - 아이템 매핑 테이블
-- Vibe 결과에 포함된 추천 아이템 목록. 결과 1개에 여러 아이템이 매핑됨.
-- match_score는 Neo4j에서 계산된 매칭 점수.
-- v3.2: recommend_reason TEXT 추가. sort_order 삭제.
-- ----------------------------------------------------------------------------
CREATE TABLE vibe_items (
    vibe_item_id BIGINT PRIMARY KEY AUTO_INCREMENT,       -- 매핑 고유 식별자
    result_id BIGINT NOT NULL,                             -- Vibe 결과 ID (FK)
    item_id BIGINT NOT NULL,                               -- 아이템 ID (FK)
    match_score DECIMAL(5,2),                              -- 매칭 점수 (Neo4j 유사도 기반)
    recommend_reason TEXT,                                 -- 추천 이유 (LLM 생성)
    is_user_liked BOOLEAN DEFAULT FALSE,                   -- 사용자가 좋아요를 눌렀는지 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 매핑 생성 시각
    UNIQUE KEY uk_result_item (result_id, item_id),
    FOREIGN KEY (result_id) REFERENCES vibe_results(result_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(item_id)
);


-- ----------------------------------------------------------------------------
-- 아카이브 Vibe 테이블
-- 사용자가 Vibe 결과를 저장한 이력. 폴더 분류 지원.
-- v3.2: is_favorite 삭제 → favorites 테이블로 분리.
-- ----------------------------------------------------------------------------
CREATE TABLE archive_vibes (
    archive_id BIGINT PRIMARY KEY AUTO_INCREMENT,         -- 아카이브 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK)
    result_id BIGINT NOT NULL,                             -- Vibe 결과 ID (FK)
    folder_id BIGINT,                                      -- 폴더 ID (FK, NULL = 미분류)
    memo VARCHAR(500),                                     -- 사용자 메모
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 저장 시각
    UNIQUE KEY uk_user_result (user_id, result_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (result_id) REFERENCES vibe_results(result_id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES archive_folders(folder_id) ON DELETE SET NULL
);


-- ----------------------------------------------------------------------------
-- 피드 테이블
-- 사용자가 Vibe 결과를 공유한 피드. 공개/비공개, 고정 기능 지원.
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE feeds (
    feed_id BIGINT PRIMARY KEY AUTO_INCREMENT,            -- 피드 고유 식별자
    user_id BIGINT NOT NULL,                               -- 작성자 ID (FK)
    result_id BIGINT NOT NULL,                             -- Vibe 결과 ID (FK)
    caption TEXT,                                          -- 피드 캡션 (사용자 작성 텍스트)
    is_public BOOLEAN DEFAULT FALSE,                       -- 공개 여부
    is_pinned BOOLEAN DEFAULT FALSE,                       -- 프로필 상단 고정 여부
    view_count INT DEFAULT 0,                              -- 조회수
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 작성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 수정 시각
    deleted_at TIMESTAMP,                                  -- 삭제 시각 (소프트 삭제)
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (result_id) REFERENCES vibe_results(result_id) ON DELETE CASCADE
);


-- ============================================================================
-- LEVEL 6: Level 0~5 테이블 참조
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 피드 반응 테이블
-- 피드에 대한 사용자 반응 (좋아요, 하트, 와우, 포근해요).
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE feed_reactions (
    reaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,        -- 반응 고유 식별자
    feed_id BIGINT NOT NULL,                               -- 피드 ID (FK)
    user_id BIGINT NOT NULL,                               -- 반응한 사용자 ID (FK)
    reaction_type ENUM('LIKE', 'LOVE', 'WOW',
                       'COZY') NOT NULL,                   -- 반응 유형
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 반응 시각
    UNIQUE KEY uk_feed_user_reaction (feed_id, user_id, reaction_type),
    FOREIGN KEY (feed_id) REFERENCES feeds(feed_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 피드 댓글 테이블
-- 피드에 대한 댓글. 대댓글(self-reference) 지원.
-- v3.2: 변동 없음.
-- ----------------------------------------------------------------------------
CREATE TABLE feed_comments (
    comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,         -- 댓글 고유 식별자
    feed_id BIGINT NOT NULL,                               -- 피드 ID (FK)
    user_id BIGINT NOT NULL,                               -- 작성자 ID (FK)
    parent_comment_id BIGINT,                              -- 대댓글 시 부모 댓글 ID (self-reference, NULL = 최상위)
    content TEXT NOT NULL,                                 -- 댓글 내용
    is_hidden BOOLEAN DEFAULT FALSE,                       -- 숨김 처리 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 작성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 수정 시각
    deleted_at TIMESTAMP,                                  -- 삭제 시각 (소프트 삭제)
    FOREIGN KEY (feed_id) REFERENCES feeds(feed_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES feed_comments(comment_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- 즐겨찾기 테이블
-- archive_vibes 및 archive_items의 즐겨찾기를 별도 관리.
-- v3.2 신규: archive_vibes.is_favorite 대체.
-- v3.2.1: archive_id nullable 변경, archive_item_id 추가 — 아이템 즐겨찾기 지원.
--         archive_id와 archive_item_id 중 정확히 하나만 NOT NULL (서비스 레이어에서 검증).
-- ----------------------------------------------------------------------------
CREATE TABLE favorites (
    favorite_id BIGINT PRIMARY KEY AUTO_INCREMENT,        -- 즐겨찾기 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK)
    archive_id BIGINT,                                     -- 아카이브 Vibe ID (FK → archive_vibes, NULL 허용)
    archive_item_id BIGINT,                                -- 아카이브 아이템 ID (FK → archive_items, NULL 허용)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 즐겨찾기 시각
    UNIQUE KEY uk_user_archive (user_id, archive_id),
    UNIQUE KEY uk_user_archive_item (user_id, archive_item_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (archive_id) REFERENCES archive_vibes(archive_id) ON DELETE CASCADE,
    FOREIGN KEY (archive_item_id) REFERENCES archive_items(archive_item_id) ON DELETE CASCADE
);


-- ============================================================================
-- LEVEL 7: Level 0~6 테이블 참조
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 댓글 좋아요 테이블
-- 피드 댓글에 대한 좋아요 반응. 좋아요만 지원 (단일 반응 유형).
-- v3.2 신규.
-- ----------------------------------------------------------------------------
CREATE TABLE comment_reactions (
    reaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,        -- 반응 고유 식별자
    comment_id BIGINT NOT NULL,                            -- 댓글 ID (FK → feed_comments)
    user_id BIGINT NOT NULL,                               -- 반응한 사용자 ID (FK)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 반응 시각
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES feed_comments(comment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ============================================================================
-- 비MVP / 보류 테이블
-- 아래 테이블은 MVP 범위 밖이거나 보류 상태입니다.
-- 추후 MVP 이후 단계에서 활성화 예정.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- [MVP 아님] 분석 리포트 테이블
-- 월간/연간 사용자 Vibe 분석 리포트 (감정 분포, 인기 키워드, 추천 아이템 등).
-- ----------------------------------------------------------------------------
CREATE TABLE reports (
    report_id BIGINT PRIMARY KEY AUTO_INCREMENT,          -- 리포트 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK)
    report_type ENUM('MONTHLY', 'YEARLY') NOT NULL,        -- 리포트 유형 (월간/연간)
    year INT NOT NULL,                                     -- 대상 연도
    month INT,                                             -- 대상 월 (연간 리포트 시 NULL)
    total_vibes INT DEFAULT 0,                             -- 해당 기간 총 Vibe 수
    active_days INT DEFAULT 0,                             -- 활동 일수
    top_mood_keyword_id BIGINT,                            -- 가장 많이 사용한 무드 키워드 ID (FK)
    top_time_id BIGINT,                                    -- 가장 많이 선택한 시간대 ID (FK)
    top_place_id BIGINT,                                   -- 가장 많이 선택한 공간 ID (FK)
    mood_distribution JSON,                                -- 무드 키워드별 분포 (JSON)
    time_distribution JSON,                                -- 시간대별 분포 (JSON)
    weekly_trend JSON,                                     -- 주차별 Vibe 추이 (JSON)
    recommended_items JSON,                                -- 추천 아이템 목록 (JSON)
    identity_type VARCHAR(100),                            -- 감성 정체성 유형 (예: '감성 탐험가')
    identity_emoji VARCHAR(10),                            -- 감성 정체성 이모지
    report_image_url VARCHAR(500),                         -- 리포트 이미지 URL
    share_card_url VARCHAR(500),                           -- 공유용 카드 이미지 URL
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 리포트 생성 시각
    UNIQUE KEY uk_user_report (user_id, report_type, year, month),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (top_mood_keyword_id) REFERENCES mood_keywords(keyword_id),
    FOREIGN KEY (top_time_id) REFERENCES time_options(time_id),
    FOREIGN KEY (top_place_id) REFERENCES place_options(place_id)
);


-- ----------------------------------------------------------------------------
-- [MVP 아님] 외부 서비스 연동 테이블
-- Spotify, Netflix 등 외부 서비스 OAuth 연동 정보.
-- ----------------------------------------------------------------------------
CREATE TABLE external_connections (
    connection_id BIGINT PRIMARY KEY AUTO_INCREMENT,      -- 연동 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK)
    service_type VARCHAR(50) NOT NULL,                     -- 서비스 유형 (예: 'SPOTIFY', 'NETFLIX', 'HUE')
    access_token TEXT,                                     -- OAuth 액세스 토큰
    refresh_token TEXT,                                    -- OAuth 리프레시 토큰
    token_expires_at TIMESTAMP,                            -- 토큰 만료 시각
    service_user_id VARCHAR(255),                          -- 외부 서비스 측 사용자 ID
    service_user_name VARCHAR(255),                        -- 외부 서비스 측 사용자명
    is_active BOOLEAN DEFAULT TRUE,                        -- 연동 활성화 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 연동 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 수정 시각
    UNIQUE KEY uk_user_service (user_id, service_type),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- [MVP 아님] 검색 이력 테이블
-- 사용자의 검색 이력 (Vibe 검색, 아이템 검색, 피드 검색, 사용자 검색).
-- ----------------------------------------------------------------------------
CREATE TABLE search_histories (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,         -- 검색 이력 고유 식별자
    user_id BIGINT NOT NULL,                               -- 사용자 ID (FK)
    search_type ENUM('VIBE', 'ITEM', 'FEED',
                     'USER') NOT NULL,                     -- 검색 유형
    keyword VARCHAR(255),                                  -- 검색 키워드
    session_id BIGINT,                                     -- 연관 Vibe 세션 ID (Vibe 검색 시, NULL 허용)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 검색 시각
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES vibe_sessions(session_id) ON DELETE SET NULL
);


-- ----------------------------------------------------------------------------
-- [MVP 아님] 인기 Vibe 트렌드 캐시 테이블
-- 일별 인기 Vibe 결과 순위. 배치 작업으로 갱신.
-- ----------------------------------------------------------------------------
CREATE TABLE trending_vibes (
    trend_id BIGINT PRIMARY KEY AUTO_INCREMENT,           -- 트렌드 고유 식별자
    result_id BIGINT NOT NULL,                             -- Vibe 결과 ID (FK)
    score DECIMAL(10,2) NOT NULL,                          -- 인기 점수 (좋아요, 조회수 등 종합)
    rank_position INT,                                     -- 순위
    trend_date DATE NOT NULL,                              -- 기준 날짜
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    UNIQUE KEY uk_result_date (result_id, trend_date),
    FOREIGN KEY (result_id) REFERENCES vibe_results(result_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- [MVP 아님] 무드 키워드 트렌드 테이블
-- 일별 무드 키워드 사용 통계. 트렌드 분석 및 인기 키워드 노출용.
-- ----------------------------------------------------------------------------
CREATE TABLE mood_trends (
    trend_id BIGINT PRIMARY KEY AUTO_INCREMENT,           -- 트렌드 고유 식별자
    keyword_id BIGINT NOT NULL,                            -- 무드 키워드 ID (FK)
    trend_date DATE NOT NULL,                              -- 기준 날짜
    usage_count INT DEFAULT 0,                             -- 해당 날짜의 사용 횟수
    growth_rate DECIMAL(5,2),                              -- 전일 대비 증가율 (%)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    UNIQUE KEY uk_keyword_date (keyword_id, trend_date),
    FOREIGN KEY (keyword_id) REFERENCES mood_keywords(keyword_id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- [보류] ETL 파이프라인 실행 이력 테이블
-- Airflow DAG 실행 로그. 1단계(수집)와 2단계(Graph 처리)를 구분하여 기록.
-- ----------------------------------------------------------------------------
CREATE TABLE etl_job_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,                 -- 로그 고유 식별자
    job_name VARCHAR(100) NOT NULL,                        -- 작업명 (예: 'movie_collect', 'music_graph_process')
    dag_id VARCHAR(100),                                   -- Airflow DAG 식별자
    task_id VARCHAR(100),                                  -- Airflow Task 식별자
    domain VARCHAR(50),                                    -- 도메인 (예: 'video', 'music', 'coffee', 'lighting')
    pipeline_stage ENUM('COLLECT', 'GRAPH_PROCESS')
        NOT NULL DEFAULT 'COLLECT',                        -- 파이프라인 단계 (COLLECT: 1단계, GRAPH_PROCESS: 2단계)
    execution_date TIMESTAMP,                              -- 예정 실행 일시
    status VARCHAR(20) NOT NULL,                           -- 실행 상태 (예: 'SUCCESS', 'FAILED', 'RUNNING')
    records_processed INTEGER DEFAULT 0,                   -- 처리 성공 건수
    records_failed INTEGER DEFAULT 0,                      -- 처리 실패 건수
    records_skipped INTEGER DEFAULT 0,                     -- 건너뛴 건수 (중복 등)
    started_at TIMESTAMP NOT NULL,                         -- 실행 시작 시각
    completed_at TIMESTAMP,                                -- 실행 완료 시각
    error_message TEXT,                                    -- 에러 발생 시 메시지
    metadata JSON,                                         -- 추가 메타정보 (파라미터, 설정값 등)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP         -- 레코드 생성 시각
);


-- ----------------------------------------------------------------------------
-- [보류] 데이터 품질 검증 결과 테이블
-- 1단계 파이프라인에서 수집 데이터의 품질 검증 실패(fail) 건만 기록.
-- v3.2: expected_value, actual_value, passed 삭제.
--       original_data JSON 추가 (검증 실패한 원본 데이터 저장).
--       fail 건만 저장하도록 설계 변경 (함수 구현 필요).
-- ----------------------------------------------------------------------------
CREATE TABLE data_quality_checks (
    check_id BIGINT PRIMARY KEY AUTO_INCREMENT,           -- 검증 결과 고유 식별자
    etl_job_id BIGINT NOT NULL,                            -- ETL 작업 ID (FK → etl_job_logs)
    check_name VARCHAR(100) NOT NULL,                      -- 검증 항목명 (예: 'null_check_title', 'range_check_intensity')
    check_type VARCHAR(50) NOT NULL,                       -- 검증 유형 (예: 'NULL_CHECK', 'RANGE_CHECK', 'FORMAT_CHECK')
    original_data JSON,                                    -- 검증 실패한 원본 데이터 (JSON)
    message TEXT,                                          -- 검증 실패 상세 메시지
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    FOREIGN KEY (etl_job_id) REFERENCES etl_job_logs(id) ON DELETE CASCADE
);


-- ----------------------------------------------------------------------------
-- [보류] 조명 실제 제품 테이블 (Phase 2 확장용)
-- lighting_details의 속성 조합에 매칭되는 실제 조명 제품 (IKEA, 오늘의집, Hue 등).
-- ----------------------------------------------------------------------------
CREATE TABLE lighting_products (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,         -- 제품 고유 식별자
    lighting_detail_id BIGINT NOT NULL,                    -- 조명 상세 ID (FK → lighting_details.item_id)
    product_name VARCHAR(255) NOT NULL,                    -- 제품명
    brand VARCHAR(100),                                    -- 브랜드 (예: 'IKEA', 'Philips')
    product_url VARCHAR(500),                              -- 구매 링크 URL
    image_url VARCHAR(500),                                -- 제품 이미지 URL
    price DECIMAL(10,2),                                   -- 가격
    currency VARCHAR(5) DEFAULT 'KRW',                     -- 통화 코드
    color_temp_min INTEGER,                                -- 제품 지원 최소 색온도
    color_temp_max INTEGER,                                -- 제품 지원 최대 색온도
    has_dimming BOOLEAN DEFAULT FALSE,                     -- 디밍(밝기 조절) 지원 여부
    has_rgb BOOLEAN DEFAULT FALSE,                         -- RGB 색상 변경 지원 여부
    source VARCHAR(50),                                    -- 데이터 출처 (예: 'ikea', 'ohouse', 'hue')
    source_product_id VARCHAR(100),                        -- 원본 소스의 제품 ID
    is_available BOOLEAN DEFAULT TRUE,                     -- 구매 가능 여부
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,        -- 레코드 생성 시각
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,                       -- 레코드 수정 시각
    FOREIGN KEY (lighting_detail_id) REFERENCES lighting_details(item_id) ON DELETE CASCADE
);


-- ============================================================================
-- 참고: Neo4j 그래프 스키마 (별도 관리)
-- ============================================================================
--
-- Neo4j에서 관리하는 노드/관계 구조 (Cypher로 정의):
--
-- [노드]
--   (:Item {item_id, category, item_key, mood_summary})
--   (:MoodKeyword {keyword_id, keyword_value, category})
--   (:Genre {name})
--   (:Artist {name, mbid})
--   (:VibePrompt {prompt_id, embedding})
--
-- [관계]
--   (:Item)-[:HAS_MOOD {confidence, tag_category}]->(:MoodKeyword)
--   (:Item)-[:BELONGS_TO]->(:Genre)
--   (:Item)-[:PERFORMED_BY]->(:Artist)
--   (:Item)-[:SIMILAR_VIBE {score}]->(:Item)  -- 크로스 도메인 유사도
--   (:MoodKeyword)-[:RELATED_TO {strength}]->(:MoodKeyword)
--   (:VibePrompt)-[:MATCHED]->(:Item)
--
-- [임베딩 벡터]
--   Item 노드에 mood_embedding, meta_embedding 프로퍼티로 저장
--   VibePrompt 노드에 prompt_embedding 프로퍼티로 저장
--   Neo4j Vector Index를 활용한 유사도 검색
--
-- ============================================================================
-- END OF SCHEMA
-- ============================================================================