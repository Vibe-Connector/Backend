-- Step 1: 기분 키워드
INSERT INTO mood_keywords (
        keyword_key,
        keyword_text,
        category,
        sort_order,
        is_active
    )
VALUES ('languid', '나른한', 'ADJECTIVE', 1, TRUE),
    ('cozy', '포근한', 'ADJECTIVE', 2, TRUE),
    ('dreamy', '몽글몽글한', 'ADJECTIVE', 3, TRUE),
    ('crisp', '청량한', 'ADJECTIVE', 4, TRUE),
    ('melancholic', '쓸쓸한', 'EMOTION', 5, TRUE),
    ('energetic', '활기찬', 'EMOTION', 6, TRUE),
    ('serene', '고요한', 'ATMOSPHERE', 7, TRUE),
    ('nostalgic', '향수 어린', 'EMOTION', 8, TRUE),
    ('focused', '몰입되는', 'ADJECTIVE', 9, TRUE),
    ('whimsical', '발랄한', 'ADJECTIVE', 10, TRUE) ON CONFLICT (keyword_key) DO NOTHING;
-- Step 2: 시간
INSERT INTO time_options (time_key, time_text, sort_order, is_active)
VALUES ('early_morning', '이른 아침', 1, TRUE),
    ('morning', '오전', 2, TRUE),
    ('afternoon', '오후', 3, TRUE),
    ('evening', '저녁', 4, TRUE),
    ('late_night', '늦은 밤', 5, TRUE),
    ('dawn', '새벽', 6, TRUE) ON CONFLICT (time_key) DO NOTHING;
-- Step 2: 날씨
INSERT INTO weather_options (
        weather_key,
        weather_text,
        icon,
        sort_order,
        is_active
    )
VALUES ('chilly', '쌀쌀한', '🥶', 1, TRUE),
    ('crisp', '상쾌한', '🌤️', 2, TRUE),
    ('rainy', '비 오는', '🌧️', 3, TRUE),
    ('sunny', '화창한', '☀️', 4, TRUE),
    ('snowy', '눈 오는', '❄️', 5, TRUE),
    ('cloudy', '흐린', '☁️', 6, TRUE) ON CONFLICT (weather_key) DO NOTHING;
-- Step 3: 공간
INSERT INTO place_options (
        place_key,
        place_text,
        icon,
        sort_order,
        is_active
    )
VALUES ('cafe', '카페 창가', '☕', 1, TRUE),
    ('home_living', '집 거실', '🏠', 2, TRUE),
    ('bedroom', '침실', '🛏️', 3, TRUE),
    ('car', '자동차 안', '🚗', 4, TRUE),
    ('park', '공원', '🌳', 5, TRUE),
    ('office', '사무실', '💼', 6, TRUE) ON CONFLICT (place_key) DO NOTHING;
-- Step 3: 동반자
INSERT INTO companion_options (
        companion_key,
        companion_text,
        icon,
        sort_order,
        is_active
    )
VALUES ('alone', '혼자', '🧘', 1, TRUE),
    ('partner', '연인과', '💑', 2, TRUE),
    ('friends', '친구들과', '👯', 3, TRUE),
    ('pet', '반려동물과', '🐾', 4, TRUE),
    ('family', '가족과', '👨‍👩‍👧', 5, TRUE),
    ('colleagues', '동료와', '🤝', 6, TRUE) ON CONFLICT (companion_key) DO NOTHING;