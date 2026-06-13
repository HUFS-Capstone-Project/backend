-- =============================================================================
-- scripts/seed-local-scarce.sql
-- 데이트 코스 "장소 부족" 시나리오용 시드 (개선 효과 시연용)
--
-- 장소 구성: FOOD 2 / CAFE 1 / ACTIVITY 1 (총 4개)
--   categorySequence = [FOOD, CAFE, ACTIVITY] 로 3코스를 생성하면
--   CAFE/ACTIVITY 는 후보가 1개뿐이므로,
--     - (기존 하드 제외 방식) 2번째·3번째 코스에서 CAFE/ACTIVITY 슬롯이 잘렸음(skip).
--     - (개선 소프트 룰)      3코스 모두 CAFE/ACTIVITY 가 채워짐(중복 허용 + 패널티).
--
-- 사용 순서 (seed-local.sql 과 동일):
--   1. 앱 구동 (PlaceTaxonomySeedDataInitializer 자동 실행 대기)
--   2. Swagger: GET /api/v1/auth/dev/master-token  → userId 확인
--   3. Swagger: POST /api/v1/rooms                 → publicId 확인
--   4. 아래 v_user_id, v_room_pid 값 수정 후 저장
--   5. 터미널에서 실행:
--        bash:       docker exec -i udidura-postgres psql -U udidura -d udidura < scripts/seed-local-scarce.sql
--        PowerShell: Get-Content scripts/seed-local-scarce.sql | docker exec -i udidura-postgres psql -U udidura -d udidura
--
-- 멱등 스크립트: ON CONFLICT DO NOTHING 사용, 중복 실행 안전
-- ⚠️ 풍부 시드(seed-local.sql)와 다른 방(room)에서 쓰는 것을 권장(장소가 합쳐지면 부족 상황이 사라짐).
-- =============================================================================

DO $$
DECLARE
    -- ★★★ 아래 두 값을 반드시 수정하세요 ★★★
    v_user_id  BIGINT := 0;                     -- ① GET /api/v1/auth/dev/master-token 응답의 userId
    v_room_pid TEXT   := 'YOUR-ROOM-PUBLIC-ID'; -- ② POST /api/v1/rooms 응답의 publicId
    -- ★★★★★★★★★★★★★★★★★★★★★★★★★★

    v_room_id        BIGINT;
    v_food_cat_id    BIGINT;
    v_cafe_cat_id    BIGINT;
    v_act_cat_id     BIGINT;

    v_tag_korean     BIGINT;
    v_tag_chinese    BIGINT;
    v_tag_bakery     BIGINT;
    v_tag_park       BIGINT;

    v_place_id       BIGINT;
    v_now            TIMESTAMPTZ := NOW();
    v_bh_expires     TIMESTAMPTZ := '2027-12-31 00:00:00+00';
    v_bh_json        TEXT;

    v_rl_food1       BIGINT;
    v_rl_food2       BIGINT;
    v_rl_cafe1       BIGINT;
    v_rl_act1        BIGINT;

BEGIN
    -- 입력 검증
    IF v_user_id = 0 THEN
        RAISE EXCEPTION '[seed-scarce] v_user_id를 실제 userId로 수정하세요.';
    END IF;
    IF v_room_pid = 'YOUR-ROOM-PUBLIC-ID' THEN
        RAISE EXCEPTION '[seed-scarce] v_room_pid를 실제 roomPublicId로 수정하세요.';
    END IF;

    -- 영업시간 JSON: 월~일 10:00-22:00
    v_bh_json := '{"daily_hours":['
        || '{"day":"월","open":"10:00","close":"22:00"},'
        || '{"day":"화","open":"10:00","close":"22:00"},'
        || '{"day":"수","open":"10:00","close":"22:00"},'
        || '{"day":"목","open":"10:00","close":"22:00"},'
        || '{"day":"금","open":"10:00","close":"22:00"},'
        || '{"day":"토","open":"10:00","close":"22:00"},'
        || '{"day":"일","open":"10:00","close":"22:00"}'
        || ']}';

    -- 룸 조회
    SELECT id INTO v_room_id FROM rooms WHERE public_id = v_room_pid;
    IF v_room_id IS NULL THEN
        RAISE EXCEPTION '[seed-scarce] 룸을 찾을 수 없습니다: %', v_room_pid;
    END IF;

    -- 카테고리 ID 조회
    SELECT id INTO v_food_cat_id FROM place_category WHERE code = 'FOOD';
    SELECT id INTO v_cafe_cat_id FROM place_category WHERE code = 'CAFE';
    SELECT id INTO v_act_cat_id  FROM place_category WHERE code = 'ACTIVITY';

    IF v_food_cat_id IS NULL OR v_cafe_cat_id IS NULL OR v_act_cat_id IS NULL THEN
        RAISE EXCEPTION '[seed-scarce] place_category를 찾을 수 없습니다. 앱을 먼저 구동해 taxonomy seed를 완료하세요.';
    END IF;

    -- 태그 ID 조회
    SELECT id INTO v_tag_korean  FROM place_tag WHERE code = 'KOREAN'  AND category_id = v_food_cat_id;
    SELECT id INTO v_tag_chinese FROM place_tag WHERE code = 'CHINESE' AND category_id = v_food_cat_id;
    SELECT id INTO v_tag_bakery  FROM place_tag WHERE code = 'BAKERY'  AND category_id = v_cafe_cat_id;
    SELECT id INTO v_tag_park    FROM place_tag WHERE code = 'PARK'    AND category_id = v_act_cat_id;

    -- 룸 멤버 등록
    INSERT INTO room_members (room_id, user_id, pinned, created_at, updated_at)
    VALUES (v_room_id, v_user_id, false, v_now, v_now)
    ON CONFLICT (room_id, user_id) DO NOTHING;

    -- =========================================================================
    -- 장소 삽입 (4개 — FOOD 2 / CAFE 1 / ACTIVITY 1)
    -- =========================================================================
    INSERT INTO places (
        source, external_place_id, kakao_place_id,
        name, category_name, category_group_code,
        address, road_address,
        latitude, longitude,
        service_category_id, service_tag_id,
        created_at, updated_at
    ) VALUES
        ('KAKAO', 'scarce_food_001', 'scarce_food_001',
         '부족시드 한식당', '음식점 > 한식', 'FD6',
         '서울 마포구 서교동 1-1', '서울 마포구 홍익로 1',
         37.553000, 126.921000,
         v_food_cat_id, v_tag_korean, v_now, v_now),

        ('KAKAO', 'scarce_food_002', 'scarce_food_002',
         '부족시드 중식당', '음식점 > 중식', 'FD6',
         '서울 마포구 연남동 2-2', '서울 마포구 동교로 2',
         37.560500, 126.928000,
         v_food_cat_id, v_tag_chinese, v_now, v_now),

        ('KAKAO', 'scarce_cafe_001', 'scarce_cafe_001',
         '부족시드 베이커리', '카페 > 베이커리', 'CE7',
         '서울 마포구 연남동 6-6', '서울 마포구 동교로 6',
         37.559800, 126.927000,
         v_cafe_cat_id, v_tag_bakery, v_now, v_now),

        ('KAKAO', 'scarce_act_001', 'scarce_act_001',
         '부족시드 공원', '관광명소 > 공원', 'AT4',
         '서울 마포구 연남동 11-1', '서울 마포구 경의로 11',
         37.558000, 126.926200,
         v_act_cat_id, v_tag_park, v_now, v_now)

    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- =========================================================================
    -- room_places 삽입
    -- =========================================================================
    FOR v_place_id IN
        SELECT id FROM places
        WHERE kakao_place_id IN (
            'scarce_food_001', 'scarce_food_002', 'scarce_cafe_001', 'scarce_act_001'
        )
    LOOP
        INSERT INTO room_places (
            room_id, place_id, created_by_user_id, added_via,
            sido_code, sido_name, sigungu_code, sigungu_name,
            created_at, updated_at
        )
        VALUES (
            v_room_id, v_place_id, v_user_id, 'EXTERNAL_SEARCH',
            '11', '서울특별시', '11440', '마포구',
            v_now, v_now
        )
        ON CONFLICT (room_id, place_id) DO NOTHING;
    END LOOP;

    -- =========================================================================
    -- place_business_hours (코스 생성 필터 통과를 위해 필수)
    -- =========================================================================
    INSERT INTO place_business_hours (
        kakao_place_id, place_name,
        business_hours_json, business_hours_status,
        business_hours_fetched_at, business_hours_expires_at,
        version, created_at, updated_at
    )
    SELECT
        p.kakao_place_id, p.name, v_bh_json, 'SUCCEEDED',
        v_now, v_bh_expires, 0, v_now, v_now
    FROM places p
    WHERE p.kakao_place_id IN (
        'scarce_food_001', 'scarce_food_002', 'scarce_cafe_001', 'scarce_act_001'
    )
    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- =========================================================================
    -- POPULAR 코스도 동작하도록 4개 장소 모두 link 부여 (likeCount 차등)
    -- =========================================================================
    INSERT INTO links (
        original_url, normalized_url,
        link_source_type, dispatch_status, status,
        like_count, version, created_at, updated_at
    ) VALUES
        ('https://www.instagram.com/p/scarce_food1/', 'https://www.instagram.com/p/scarce_food1/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED', 1000, 0, v_now, v_now),
        ('https://www.instagram.com/p/scarce_food2/', 'https://www.instagram.com/p/scarce_food2/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  200, 0, v_now, v_now),
        ('https://www.instagram.com/p/scarce_cafe1/', 'https://www.instagram.com/p/scarce_cafe1/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  900, 0, v_now, v_now),
        ('https://www.instagram.com/p/scarce_act1/',  'https://www.instagram.com/p/scarce_act1/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  800, 0, v_now, v_now)
    ON CONFLICT (normalized_url) DO NOTHING;

    INSERT INTO room_links (room_id, link_id, created_at, updated_at)
        SELECT v_room_id, id, v_now, v_now FROM links
        WHERE normalized_url IN (
            'https://www.instagram.com/p/scarce_food1/',
            'https://www.instagram.com/p/scarce_food2/',
            'https://www.instagram.com/p/scarce_cafe1/',
            'https://www.instagram.com/p/scarce_act1/'
        )
    ON CONFLICT (room_id, link_id) DO NOTHING;

    SELECT rl.id INTO v_rl_food1 FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/scarce_food1/';
    SELECT rl.id INTO v_rl_food2 FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/scarce_food2/';
    SELECT rl.id INTO v_rl_cafe1 FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/scarce_cafe1/';
    SELECT rl.id INTO v_rl_act1  FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/scarce_act1/';

    UPDATE room_places SET origin_room_link_id = v_rl_food1
        WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'scarce_food_001');
    UPDATE room_places SET origin_room_link_id = v_rl_food2
        WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'scarce_food_002');
    UPDATE room_places SET origin_room_link_id = v_rl_cafe1
        WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'scarce_cafe_001');
    UPDATE room_places SET origin_room_link_id = v_rl_act1
        WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'scarce_act_001');

    RAISE NOTICE '[seed-scarce] 완료 — 장소 4개(FOOD2/CAFE1/ACT1), 영업시간 4개, links 4개 추가됨 (room: %)', v_room_pid;
END $$;
