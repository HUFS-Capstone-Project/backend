-- =============================================================================
-- scripts/seed-local.sql
-- 데이트 코스 로컬 테스트용 장소 Mock 데이터 시드
--
-- 사용 순서:
--   1. 앱 구동 (PlaceTaxonomySeedDataInitializer 자동 실행 대기)
--   2. Swagger: GET /api/v1/auth/dev/master-token  → userId 확인
--   3. Swagger: POST /api/v1/rooms                 → publicId 확인
--   4. 아래 v_user_id, v_room_pid 값 수정 후 저장
--   5. 터미널에서 실행:
--        docker exec -i udidura-postgres psql -U udidura -d udidura < scripts/seed-local.sql
--
-- 멱등 스크립트: ON CONFLICT DO NOTHING 사용, 중복 실행 안전
-- Docker 재시작 후에도 named volume(udidura_pg_data)이 데이터를 유지함
-- =============================================================================

DO $$
DECLARE
    -- ★★★ 아래 두 값을 반드시 수정하세요 ★★★
    v_user_id  BIGINT := 0;                     -- ① GET /api/v1/auth/dev/master-token 응답의 userId
    v_room_pid TEXT   := 'INPUT ROOM_ID HERE'; -- ② POST /api/v1/rooms 응답의 publicId
    -- ★★★★★★★★★★★★★★★★★★★★★★★★★★

    v_room_id        BIGINT;
    v_food_cat_id    BIGINT;
    v_cafe_cat_id    BIGINT;
    v_act_cat_id     BIGINT;

    v_tag_korean     BIGINT;
    v_tag_chinese    BIGINT;
    v_tag_japanese   BIGINT;
    v_tag_western    BIGINT;
    v_tag_bar        BIGINT;

    v_tag_bakery     BIGINT;
    v_tag_coffee_des BIGINT;
    v_tag_cafe_misc  BIGINT;

    v_tag_escape     BIGINT;
    v_tag_photo      BIGINT;
    v_tag_park       BIGINT;

    v_place_id       BIGINT;
    v_now            TIMESTAMPTZ := NOW();
    v_bh_expires     TIMESTAMPTZ := '2027-12-31 00:00:00+00';
    v_bh_json        TEXT;

BEGIN
    -- 입력 검증
    IF v_user_id = 0 THEN
        RAISE EXCEPTION '[seed] v_user_id를 실제 userId로 수정하세요.';
    END IF;
    IF v_room_pid = 'YOUR-ROOM-PUBLIC-ID' THEN
        RAISE EXCEPTION '[seed] v_room_pid를 실제 roomPublicId로 수정하세요.';
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
        RAISE EXCEPTION '[seed] 룸을 찾을 수 없습니다: %', v_room_pid;
    END IF;

    -- 카테고리 ID 조회 (앱 구동 시 PlaceTaxonomySeedDataInitializer가 자동 삽입)
    SELECT id INTO v_food_cat_id FROM place_category WHERE code = 'FOOD';
    SELECT id INTO v_cafe_cat_id FROM place_category WHERE code = 'CAFE';
    SELECT id INTO v_act_cat_id  FROM place_category WHERE code = 'ACTIVITY';

    IF v_food_cat_id IS NULL OR v_cafe_cat_id IS NULL OR v_act_cat_id IS NULL THEN
        RAISE EXCEPTION '[seed] place_category를 찾을 수 없습니다. 앱을 먼저 구동해 taxonomy seed를 완료하세요.';
    END IF;

    -- 음식점 태그 ID 조회
    SELECT id INTO v_tag_korean   FROM place_tag WHERE code = 'KOREAN'   AND category_id = v_food_cat_id;
    SELECT id INTO v_tag_chinese  FROM place_tag WHERE code = 'CHINESE'  AND category_id = v_food_cat_id;
    SELECT id INTO v_tag_japanese FROM place_tag WHERE code = 'JAPANESE' AND category_id = v_food_cat_id;
    SELECT id INTO v_tag_western  FROM place_tag WHERE code = 'WESTERN'  AND category_id = v_food_cat_id;
    SELECT id INTO v_tag_bar      FROM place_tag WHERE code = 'BAR'      AND category_id = v_food_cat_id;

    -- 카페 태그 ID 조회
    SELECT id INTO v_tag_bakery     FROM place_tag WHERE code = 'BAKERY'         AND category_id = v_cafe_cat_id;
    SELECT id INTO v_tag_coffee_des FROM place_tag WHERE code = 'COFFEE_DESSERT' AND category_id = v_cafe_cat_id;
    SELECT id INTO v_tag_cafe_misc  FROM place_tag WHERE code = 'MISC'           AND category_id = v_cafe_cat_id;

    -- 활동 태그 ID 조회
    SELECT id INTO v_tag_escape FROM place_tag WHERE code = 'ESCAPE_ROOM_CAFE' AND category_id = v_act_cat_id;
    SELECT id INTO v_tag_photo  FROM place_tag WHERE code = 'PHOTO_STUDIO'     AND category_id = v_act_cat_id;
    SELECT id INTO v_tag_park   FROM place_tag WHERE code = 'PARK'             AND category_id = v_act_cat_id;

    -- =========================================================================
    -- 룸 멤버 등록
    -- =========================================================================
    INSERT INTO room_members (room_id, user_id, pinned, created_at, updated_at)
    VALUES (v_room_id, v_user_id, false, v_now, v_now)
    ON CONFLICT (room_id, user_id) DO NOTHING;

    -- =========================================================================
    -- 장소 삽입 (11개 — 홍대·연남동 일대)
    -- =========================================================================

    -- FOOD (5개)
    INSERT INTO places (
        source, external_place_id, kakao_place_id,
        name, category_name, category_group_code,
        address, road_address,
        latitude, longitude,
        service_category_id, service_tag_id,
        created_at, updated_at
    ) VALUES
        ('KAKAO', 'mock_food_001', 'mock_food_001',
         '홍대 행복 삼겹살', '음식점 > 한식', 'FD6',
         '서울 마포구 서교동 1-1', '서울 마포구 홍익로 1',
         37.553000, 126.921000,
         v_food_cat_id, v_tag_korean, v_now, v_now),

        ('KAKAO', 'mock_food_002', 'mock_food_002',
         '연남동 신룡 중화요리', '음식점 > 중식', 'FD6',
         '서울 마포구 연남동 2-2', '서울 마포구 동교로 2',
         37.560500, 126.928000,
         v_food_cat_id, v_tag_chinese, v_now, v_now),

        ('KAKAO', 'mock_food_003', 'mock_food_003',
         '홍대입구 츠키 라멘', '음식점 > 일식', 'FD6',
         '서울 마포구 서교동 3-3', '서울 마포구 홍익로 3',
         37.554800, 126.922500,
         v_food_cat_id, v_tag_japanese, v_now, v_now),

        ('KAKAO', 'mock_food_004', 'mock_food_004',
         '홍대 버거앤비어', '음식점 > 양식', 'FD6',
         '서울 마포구 서교동 4-4', '서울 마포구 홍익로 4',
         37.552500, 126.923500,
         v_food_cat_id, v_tag_western, v_now, v_now),

        ('KAKAO', 'mock_food_005', 'mock_food_005',
         '연남동 포차마당', '음식점 > 술집', 'FD6',
         '서울 마포구 연남동 5-5', '서울 마포구 동교로 5',
         37.561200, 126.926500,
         v_food_cat_id, v_tag_bar, v_now, v_now)

    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- CAFE (3개)
    INSERT INTO places (
        source, external_place_id, kakao_place_id,
        name, category_name, category_group_code,
        address, road_address,
        latitude, longitude,
        service_category_id, service_tag_id,
        created_at, updated_at
    ) VALUES
        ('KAKAO', 'mock_cafe_001', 'mock_cafe_001',
         '연남동 노을빛 베이커리', '카페 > 베이커리', 'CE7',
         '서울 마포구 연남동 6-6', '서울 마포구 동교로 6',
         37.559800, 126.927000,
         v_cafe_cat_id, v_tag_bakery, v_now, v_now),

        ('KAKAO', 'mock_cafe_002', 'mock_cafe_002',
         '홍대 달콤 스위트 카페', '카페 > 디저트', 'CE7',
         '서울 마포구 서교동 7-7', '서울 마포구 홍익로 7',
         37.554200, 126.921800,
         v_cafe_cat_id, v_tag_coffee_des, v_now, v_now),

        ('KAKAO', 'mock_cafe_003', 'mock_cafe_003',
         '경의선 커피인더숲', '카페', 'CE7',
         '서울 마포구 연남동 8-8', '서울 마포구 경의로 8',
         37.558800, 126.925500,
         v_cafe_cat_id, v_tag_cafe_misc, v_now, v_now)

    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- ACTIVITY (3개)
    INSERT INTO places (
        source, external_place_id, kakao_place_id,
        name, category_name, category_group_code,
        address, road_address,
        latitude, longitude,
        service_category_id, service_tag_id,
        created_at, updated_at
    ) VALUES
        ('KAKAO', 'mock_act_001', 'mock_act_001',
         '홍대 미로탈출 방탈출카페', '여가 > 방탈출카페', 'AT4',
         '서울 마포구 서교동 9-9', '서울 마포구 홍익로 9',
         37.553500, 126.922800,
         v_act_cat_id, v_tag_escape, v_now, v_now),

        ('KAKAO', 'mock_act_002', 'mock_act_002',
         '홍대 온필름 사진관', '여가 > 사진관', 'AT4',
         '서울 마포구 서교동 10-1', '서울 마포구 홍익로 10',
         37.555200, 126.922200,
         v_act_cat_id, v_tag_photo, v_now, v_now),

        ('KAKAO', 'mock_act_003', 'mock_act_003',
         '경의선숲길 공원', '관광명소 > 공원', 'AT4',
         '서울 마포구 연남동 11-1', '서울 마포구 경의로 11',
         37.558000, 126.926200,
         v_act_cat_id, v_tag_park, v_now, v_now)

    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- =========================================================================
    -- room_places 삽입 (위에서 삽입한 11개 장소를 룸에 연결)
    -- =========================================================================
    FOR v_place_id IN
        SELECT id FROM places
        WHERE kakao_place_id IN (
            'mock_food_001', 'mock_food_002', 'mock_food_003', 'mock_food_004', 'mock_food_005',
            'mock_cafe_001', 'mock_cafe_002', 'mock_cafe_003',
            'mock_act_001',  'mock_act_002',  'mock_act_003'
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
    -- place_business_hours 삽입 (코스 생성 필터 통과를 위해 필수)
    -- status=SUCCEEDED, expires_at=2027-12-31 로 모든 시간대 영업 중으로 설정
    -- =========================================================================
    INSERT INTO place_business_hours (
        kakao_place_id, place_name,
        business_hours_json, business_hours_status,
        business_hours_fetched_at, business_hours_expires_at,
        version, created_at, updated_at
    )
    SELECT
        p.kakao_place_id,
        p.name,
        v_bh_json,
        'SUCCEEDED',
        v_now,
        v_bh_expires,
        0,
        v_now,
        v_now
    FROM places p
    WHERE p.kakao_place_id IN (
        'mock_food_001', 'mock_food_002', 'mock_food_003', 'mock_food_004', 'mock_food_005',
        'mock_cafe_001', 'mock_cafe_002', 'mock_cafe_003',
        'mock_act_001',  'mock_act_002',  'mock_act_003'
    )
    ON CONFLICT (kakao_place_id) DO NOTHING;

    RAISE NOTICE '[seed] 완료 — 장소 11개, 영업시간 11개, room_places 삽입됨 (room: %)', v_room_pid;
END $$;
