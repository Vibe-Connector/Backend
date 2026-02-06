-- 선택지 마스터 테이블들
CREATE TABLE IF NOT EXISTS mood_keywords (
    keyword_id BIGSERIAL PRIMARY KEY,
    keyword_key VARCHAR(50) UNIQUE NOT NULL,
    keyword_text VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL DEFAULT 'ADJECTIVE',
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS time_options (
    time_id BIGSERIAL PRIMARY KEY,
    time_key VARCHAR(50) UNIQUE NOT NULL,
    time_text VARCHAR(100) NOT NULL,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS weather_options (
    weather_id BIGSERIAL PRIMARY KEY,
    weather_key VARCHAR(50) UNIQUE NOT NULL,
    weather_text VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS place_options (
    place_id BIGSERIAL PRIMARY KEY,
    place_key VARCHAR(50) UNIQUE NOT NULL,
    place_text VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS companion_options (
    companion_id BIGSERIAL PRIMARY KEY,
    companion_key VARCHAR(50) UNIQUE NOT NULL,
    companion_text VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

-- Vibe 요청 + 결과 통합 테이블
CREATE TABLE IF NOT EXISTS vibe_requests (
    vibe_id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    time_id BIGINT REFERENCES time_options(time_id),
    weather_id BIGINT REFERENCES weather_options(weather_id),
    place_id BIGINT REFERENCES place_options(place_id),
    companion_id BIGINT REFERENCES companion_options(companion_id),
    final_prompt TEXT,
    result_phrase TEXT,
    result_analysis TEXT,
    processing_time_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Step 1: 복수 기분 선택 (N:M)
CREATE TABLE IF NOT EXISTS vibe_request_moods (
    id BIGSERIAL PRIMARY KEY,
    vibe_id BIGINT NOT NULL REFERENCES vibe_requests(vibe_id) ON DELETE CASCADE,
    keyword_id BIGINT NOT NULL REFERENCES mood_keywords(keyword_id),
    UNIQUE(vibe_id, keyword_id)
);
