-- Step 1: 기분 키워드
INSERT INTO mood_keywords (keyword_key, keyword_text, category, sort_order) VALUES
('languid', '나른한', 'ADJECTIVE', 1),
('cozy', '포근한', 'ADJECTIVE', 2),
('dreamy', '몽글몽글한', 'ADJECTIVE', 3),
('crisp', '청량한', 'ADJECTIVE', 4),
('melancholic', '쓸쓸한', 'EMOTION', 5),
('energetic', '활기찬', 'EMOTION', 6),
('serene', '고요한', 'ATMOSPHERE', 7),
('nostalgic', '향수 어린', 'EMOTION', 8),
('focused', '몰입되는', 'ADJECTIVE', 9),
('whimsical', '발랄한', 'ADJECTIVE', 10)
ON CONFLICT (keyword_key) DO NOTHING;

-- Step 2: 시간
INSERT INTO time_options (time_key, time_text, sort_order) VALUES
('early_morning', '이른 아침', 1),
('morning', '오전', 2),
('afternoon', '오후', 3),
('evening', '저녁', 4),
('late_night', '늦은 밤', 5),
('dawn', '새벽', 6)
ON CONFLICT (time_key) DO NOTHING;

-- Step 2: 날씨
INSERT INTO weather_options (weather_key, weather_text, icon, sort_order) VALUES
('chilly', '쌀쌀한', '🥶', 1),
('crisp', '상쾌한', '🌤️', 2),
('rainy', '비 오는', '🌧️', 3),
('sunny', '화창한', '☀️', 4),
('snowy', '눈 오는', '❄️', 5),
('cloudy', '흐린', '☁️', 6)
ON CONFLICT (weather_key) DO NOTHING;

-- Step 3: 공간
INSERT INTO place_options (place_key, place_text, icon, sort_order) VALUES
('cafe', '카페 창가', '☕', 1),
('home_living', '집 거실', '🏠', 2),
('bedroom', '침실', '🛏️', 3),
('car', '자동차 안', '🚗', 4),
('park', '공원', '🌳', 5),
('office', '사무실', '💼', 6)
ON CONFLICT (place_key) DO NOTHING;

-- Step 3: 동반자
INSERT INTO companion_options (companion_key, companion_text, icon, sort_order) VALUES
('alone', '혼자', '🧘', 1),
('partner', '연인과', '💑', 2),
('friends', '친구들과', '👯', 3),
('pet', '반려동물과', '🐾', 4),
('family', '가족과', '👨‍👩‍👧', 5),
('colleagues', '동료와', '🤝', 6)
ON CONFLICT (companion_key) DO NOTHING;
