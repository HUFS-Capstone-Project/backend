-- =============================================================================
-- scripts/seed-datecourse-test.sql
-- 데이트 코스 전체 워크플로(생성·저장·수정·삭제) Swagger 테스트용 확장 Mock 데이터
--
-- 기존 seed-local.sql(11개)보다 장소 수를 대폭 늘려
-- 여러 카테고리 조합·모드(GENERAL/TRENDY/POPULAR) 테스트와
-- 수정·삭제 시나리오를 원활히 진행할 수 있습니다.
--
-- 사용 순서:
--   1. 앱 구동 (PlaceTaxonomySeedDataInitializer 자동 실행 대기)
--   2. Swagger: GET /api/v1/auth/dev/master-token  → userId 확인
--   3. Swagger: POST /api/v1/rooms                 → publicId 확인
--   4. 아래 v_user_id, v_room_pid 값 수정 후 저장
--   5. 터미널에서 실행:
--        PowerShell: Get-Content scripts/seed-datecourse-test.sql | docker exec -i udidura-postgres psql -U udidura -d udidura
--        bash:       docker exec -i udidura-postgres psql -U udidura -d udidura < scripts/seed-datecourse-test.sql
--
-- 멱등 스크립트: ON CONFLICT DO NOTHING, 중복 실행 안전
-- =============================================================================

DO $$
DECLARE
    -- ★★★ 아래 두 값을 반드시 수정하세요 ★★★
    v_user_id  BIGINT := 1;                     -- ① GET /api/v1/auth/dev/master-token 응답의 userId
    v_room_pid TEXT   := '7cf75510-8c5d-4fb6-8f98-1f98f77669c6'; -- ② POST /api/v1/rooms 응답의 publicId
    -- ★★★★★★★★★★★★★★★★★★★★★★★★★★

    v_room_id        BIGINT;
    v_food_cat_id    BIGINT;
    v_cafe_cat_id    BIGINT;
    v_act_cat_id     BIGINT;

    -- 음식점 태그
    v_tag_korean     BIGINT;
    v_tag_chinese    BIGINT;
    v_tag_japanese   BIGINT;
    v_tag_western    BIGINT;
    v_tag_bar        BIGINT;

    -- 카페 태그
    v_tag_bakery     BIGINT;
    v_tag_coffee_des BIGINT;
    v_tag_cafe_misc  BIGINT;

    -- 활동 태그
    v_tag_escape     BIGINT;
    v_tag_photo      BIGINT;
    v_tag_park       BIGINT;

    v_place_id       BIGINT;
    v_now            TIMESTAMPTZ := NOW();
    v_bh_expires     TIMESTAMPTZ := '2027-12-31 00:00:00+00';
    v_bh_json        TEXT;

    -- POPULAR 테스트용 link ID 변수
    v_link_id        BIGINT;

BEGIN
    -- 입력 검증
    IF v_user_id = 0 THEN
        RAISE EXCEPTION '[seed] v_user_id를 실제 userId로 수정하세요.';
    END IF;
    IF v_room_pid = 'YOUR-ROOM-PUBLIC-ID' THEN
        RAISE EXCEPTION '[seed] v_room_pid를 실제 roomPublicId로 수정하세요.';
    END IF;

    -- 영업시간 JSON: 월~일 11:00-23:00
    v_bh_json := '{"daily_hours":['
        || '{"day":"월","open":"11:00","close":"23:00"},'
        || '{"day":"화","open":"11:00","close":"23:00"},'
        || '{"day":"수","open":"11:00","close":"23:00"},'
        || '{"day":"목","open":"11:00","close":"23:00"},'
        || '{"day":"금","open":"11:00","close":"23:00"},'
        || '{"day":"토","open":"11:00","close":"23:00"},'
        || '{"day":"일","open":"11:00","close":"23:00"}'
        || ']}';

    -- 룸 조회
    SELECT id INTO v_room_id FROM rooms WHERE public_id = v_room_pid;
    IF v_room_id IS NULL THEN
        RAISE EXCEPTION '[seed] 룸을 찾을 수 없습니다: %', v_room_pid;
    END IF;

    -- 카테고리 ID 조회
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
    -- 장소 삽입 — 총 27개 (홍대·연남동·합정 일대)
    -- FOOD 10개 / CAFE 9개 / ACTIVITY 8개
    -- =========================================================================

    -- ------------------------------------------------------------------
    -- FOOD 10개
    -- ------------------------------------------------------------------
    INSERT INTO places (
        source, external_place_id, kakao_place_id,
        name, category_name, category_group_code,
        address, road_address,
        latitude, longitude,
        service_category_id, service_tag_id,
        created_at, updated_at
    ) VALUES
        -- 한식 (3)
        ('KAKAO', 'ext_food_k01', 'ext_food_k01',
         '홍대 황금 삼겹살', '음식점 > 한식', 'FD6',
         '서울 마포구 서교동 101-1', '서울 마포구 홍익로 101',
         37.553100, 126.921100, v_food_cat_id, v_tag_korean, v_now, v_now),

        ('KAKAO', 'ext_food_k02', 'ext_food_k02',
         '연남동 소담 한정식', '음식점 > 한식', 'FD6',
         '서울 마포구 연남동 102-2', '서울 마포구 동교로 102',
         37.560600, 126.928100, v_food_cat_id, v_tag_korean, v_now, v_now),

        ('KAKAO', 'ext_food_k03', 'ext_food_k03',
         '합정 솥뚜껑 갈비', '음식점 > 한식', 'FD6',
         '서울 마포구 합정동 103-3', '서울 마포구 양화로 103',
         37.549200, 126.913800, v_food_cat_id, v_tag_korean, v_now, v_now),

        -- 중식 (2)
        ('KAKAO', 'ext_food_c01', 'ext_food_c01',
         '연남동 팔선 중화루', '음식점 > 중식', 'FD6',
         '서울 마포구 연남동 104-4', '서울 마포구 동교로 104',
         37.560500, 126.927900, v_food_cat_id, v_tag_chinese, v_now, v_now),

        ('KAKAO', 'ext_food_c02', 'ext_food_c02',
         '홍대 만리장성 짬뽕', '음식점 > 중식', 'FD6',
         '서울 마포구 서교동 105-5', '서울 마포구 홍익로 105',
         37.553500, 126.922300, v_food_cat_id, v_tag_chinese, v_now, v_now),

        -- 일식 (2)
        ('KAKAO', 'ext_food_j01', 'ext_food_j01',
         '홍대입구 사쿠라 라멘', '음식점 > 일식', 'FD6',
         '서울 마포구 서교동 106-6', '서울 마포구 홍익로 106',
         37.554700, 126.922400, v_food_cat_id, v_tag_japanese, v_now, v_now),

        ('KAKAO', 'ext_food_j02', 'ext_food_j02',
         '합정 스시야 오마카세', '음식점 > 일식', 'FD6',
         '서울 마포구 합정동 107-7', '서울 마포구 양화로 107',
         37.549000, 126.913500, v_food_cat_id, v_tag_japanese, v_now, v_now),

        -- 양식 (2)
        ('KAKAO', 'ext_food_w01', 'ext_food_w01',
         '홍대 파스타 비앙코', '음식점 > 양식', 'FD6',
         '서울 마포구 서교동 108-8', '서울 마포구 홍익로 108',
         37.552600, 126.923400, v_food_cat_id, v_tag_western, v_now, v_now),

        ('KAKAO', 'ext_food_w02', 'ext_food_w02',
         '연남동 바베큐 하우스', '음식점 > 양식', 'FD6',
         '서울 마포구 연남동 109-9', '서울 마포구 동교로 109',
         37.561100, 126.926600, v_food_cat_id, v_tag_western, v_now, v_now),

        -- 술집 (1)
        ('KAKAO', 'ext_food_b01', 'ext_food_b01',
         '홍대 루프탑 포차', '음식점 > 술집', 'FD6',
         '서울 마포구 서교동 110-1', '서울 마포구 홍익로 110',
         37.552900, 126.921700, v_food_cat_id, v_tag_bar, v_now, v_now)

    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- ------------------------------------------------------------------
    -- CAFE 9개
    -- ------------------------------------------------------------------
    INSERT INTO places (
        source, external_place_id, kakao_place_id,
        name, category_name, category_group_code,
        address, road_address,
        latitude, longitude,
        service_category_id, service_tag_id,
        created_at, updated_at
    ) VALUES
        -- 베이커리 (3)
        ('KAKAO', 'ext_cafe_bk01', 'ext_cafe_bk01',
         '연남동 밀가루 베이커리', '카페 > 베이커리', 'CE7',
         '서울 마포구 연남동 201-1', '서울 마포구 동교로 201',
         37.559900, 126.927100, v_cafe_cat_id, v_tag_bakery, v_now, v_now),

        ('KAKAO', 'ext_cafe_bk02', 'ext_cafe_bk02',
         '홍대 뮤제오 브레드', '카페 > 베이커리', 'CE7',
         '서울 마포구 서교동 202-2', '서울 마포구 홍익로 202',
         37.553800, 126.921600, v_cafe_cat_id, v_tag_bakery, v_now, v_now),

        ('KAKAO', 'ext_cafe_bk03', 'ext_cafe_bk03',
         '합정 오후의 빵집', '카페 > 베이커리', 'CE7',
         '서울 마포구 합정동 203-3', '서울 마포구 양화로 203',
         37.549600, 126.914200, v_cafe_cat_id, v_tag_bakery, v_now, v_now),

        -- 커피/디저트 (3)
        ('KAKAO', 'ext_cafe_cd01', 'ext_cafe_cd01',
         '홍대 스윗로드 디저트카페', '카페 > 디저트', 'CE7',
         '서울 마포구 서교동 204-4', '서울 마포구 홍익로 204',
         37.554300, 126.921900, v_cafe_cat_id, v_tag_coffee_des, v_now, v_now),

        ('KAKAO', 'ext_cafe_cd02', 'ext_cafe_cd02',
         '연남동 달달 크레이프', '카페 > 디저트', 'CE7',
         '서울 마포구 연남동 205-5', '서울 마포구 동교로 205',
         37.560200, 126.928300, v_cafe_cat_id, v_tag_coffee_des, v_now, v_now),

        ('KAKAO', 'ext_cafe_cd03', 'ext_cafe_cd03',
         '합정 블루밍 커피', '카페 > 디저트', 'CE7',
         '서울 마포구 합정동 206-6', '서울 마포구 양화로 206',
         37.549100, 126.913100, v_cafe_cat_id, v_tag_coffee_des, v_now, v_now),

        -- 기타 카페 (3)
        ('KAKAO', 'ext_cafe_m01', 'ext_cafe_m01',
         '경의선숲길 커피스탠드', '카페', 'CE7',
         '서울 마포구 연남동 207-7', '서울 마포구 경의로 207',
         37.558900, 126.925600, v_cafe_cat_id, v_tag_cafe_misc, v_now, v_now),

        ('KAKAO', 'ext_cafe_m02', 'ext_cafe_m02',
         '홍대 테라스 아라비카', '카페', 'CE7',
         '서울 마포구 서교동 208-8', '서울 마포구 홍익로 208',
         37.553200, 126.920900, v_cafe_cat_id, v_tag_cafe_misc, v_now, v_now),

        ('KAKAO', 'ext_cafe_m03', 'ext_cafe_m03',
         '합정 북카페 프롤로그', '카페', 'CE7',
         '서울 마포구 합정동 209-9', '서울 마포구 양화로 209',
         37.548700, 126.912800, v_cafe_cat_id, v_tag_cafe_misc, v_now, v_now)

    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- ------------------------------------------------------------------
    -- ACTIVITY 8개
    -- ------------------------------------------------------------------
    INSERT INTO places (
        source, external_place_id, kakao_place_id,
        name, category_name, category_group_code,
        address, road_address,
        latitude, longitude,
        service_category_id, service_tag_id,
        created_at, updated_at
    ) VALUES
        -- 방탈출 (3)
        ('KAKAO', 'ext_act_er01', 'ext_act_er01',
         '홍대 코드네임 방탈출', '여가 > 방탈출카페', 'AT4',
         '서울 마포구 서교동 301-1', '서울 마포구 홍익로 301',
         37.553600, 126.922900, v_act_cat_id, v_tag_escape, v_now, v_now),

        ('KAKAO', 'ext_act_er02', 'ext_act_er02',
         '홍대 시크릿 도어 방탈출', '여가 > 방탈출카페', 'AT4',
         '서울 마포구 서교동 302-2', '서울 마포구 홍익로 302',
         37.554100, 126.922100, v_act_cat_id, v_tag_escape, v_now, v_now),

        ('KAKAO', 'ext_act_er03', 'ext_act_er03',
         '합정 키 마스터 방탈출', '여가 > 방탈출카페', 'AT4',
         '서울 마포구 합정동 303-3', '서울 마포구 양화로 303',
         37.549300, 126.914000, v_act_cat_id, v_tag_escape, v_now, v_now),

        -- 사진관 (3)
        ('KAKAO', 'ext_act_ps01', 'ext_act_ps01',
         '홍대 필름앤필 사진관', '여가 > 사진관', 'AT4',
         '서울 마포구 서교동 304-4', '서울 마포구 홍익로 304',
         37.555100, 126.922300, v_act_cat_id, v_tag_photo, v_now, v_now),

        ('KAKAO', 'ext_act_ps02', 'ext_act_ps02',
         '연남동 포토그레이 스튜디오', '여가 > 사진관', 'AT4',
         '서울 마포구 연남동 305-5', '서울 마포구 동교로 305',
         37.560800, 126.927500, v_act_cat_id, v_tag_photo, v_now, v_now),

        ('KAKAO', 'ext_act_ps03', 'ext_act_ps03',
         '합정 레트로 스냅 사진관', '여가 > 사진관', 'AT4',
         '서울 마포구 합정동 306-6', '서울 마포구 양화로 306',
         37.548900, 126.913600, v_act_cat_id, v_tag_photo, v_now, v_now),

        -- 공원 (2)
        ('KAKAO', 'ext_act_pk01', 'ext_act_pk01',
         '경의선숲길 공원 연남구간', '관광명소 > 공원', 'AT4',
         '서울 마포구 연남동 307-7', '서울 마포구 경의로 307',
         37.558100, 126.926100, v_act_cat_id, v_tag_park, v_now, v_now),

        ('KAKAO', 'ext_act_pk02', 'ext_act_pk02',
         '합정 한강공원 접근로', '관광명소 > 공원', 'AT4',
         '서울 마포구 합정동 308-8', '서울 마포구 양화로 308',
         37.548500, 126.912100, v_act_cat_id, v_tag_park, v_now, v_now)

    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- =========================================================================
    -- room_places 삽입 — 위 27개 장소를 모두 룸에 연결
    -- =========================================================================
    FOR v_place_id IN
        SELECT id FROM places
        WHERE kakao_place_id IN (
            'ext_food_k01','ext_food_k02','ext_food_k03',
            'ext_food_c01','ext_food_c02',
            'ext_food_j01','ext_food_j02',
            'ext_food_w01','ext_food_w02',
            'ext_food_b01',
            'ext_cafe_bk01','ext_cafe_bk02','ext_cafe_bk03',
            'ext_cafe_cd01','ext_cafe_cd02','ext_cafe_cd03',
            'ext_cafe_m01','ext_cafe_m02','ext_cafe_m03',
            'ext_act_er01','ext_act_er02','ext_act_er03',
            'ext_act_ps01','ext_act_ps02','ext_act_ps03',
            'ext_act_pk01','ext_act_pk02'
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
    -- place_business_hours 삽입 — 코스 생성 필터 통과 필수
    -- status=SUCCEEDED, expires_at=2027-12-31
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
        'ext_food_k01','ext_food_k02','ext_food_k03',
        'ext_food_c01','ext_food_c02',
        'ext_food_j01','ext_food_j02',
        'ext_food_w01','ext_food_w02',
        'ext_food_b01',
        'ext_cafe_bk01','ext_cafe_bk02','ext_cafe_bk03',
        'ext_cafe_cd01','ext_cafe_cd02','ext_cafe_cd03',
        'ext_cafe_m01','ext_cafe_m02','ext_cafe_m03',
        'ext_act_er01','ext_act_er02','ext_act_er03',
        'ext_act_ps01','ext_act_ps02','ext_act_ps03',
        'ext_act_pk01','ext_act_pk02'
    )
    ON CONFLICT (kakao_place_id) DO NOTHING;

    -- =========================================================================
    -- POPULAR 코스 테스트용: links + room_links + origin_room_link_id 업데이트
    --
    -- 각 카테고리별 상위 장소에 Instagram mock likeCount 부여:
    --   FOOD:     ext_food_k01 → 2000 likes (POPULAR 최상위)
    --             ext_food_j01 →  800 likes
    --             ext_food_w01 →  300 likes
    --   CAFE:     ext_cafe_bk01→ 1500 likes (POPULAR 최상위)
    --             ext_cafe_cd01→  600 likes
    --             ext_cafe_m01 →  200 likes
    --   ACTIVITY: ext_act_er01 → 1200 likes (POPULAR 최상위)
    --             ext_act_ps01 →  500 likes
    --             ext_act_pk01 →  150 likes
    -- =========================================================================
    INSERT INTO links (
        original_url, normalized_url,
        link_source_type, dispatch_status, status,
        like_count, version, created_at, updated_at
    ) VALUES
        ('https://www.instagram.com/p/ext_food_k01_hi/', 'https://www.instagram.com/p/ext_food_k01_hi/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED', 2000, 0, v_now, v_now),
        ('https://www.instagram.com/p/ext_food_j01_md/', 'https://www.instagram.com/p/ext_food_j01_md/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  800, 0, v_now, v_now),
        ('https://www.instagram.com/p/ext_food_w01_lo/', 'https://www.instagram.com/p/ext_food_w01_lo/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  300, 0, v_now, v_now),
        ('https://www.instagram.com/p/ext_cafe_bk01_hi/', 'https://www.instagram.com/p/ext_cafe_bk01_hi/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED', 1500, 0, v_now, v_now),
        ('https://www.instagram.com/p/ext_cafe_cd01_md/', 'https://www.instagram.com/p/ext_cafe_cd01_md/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  600, 0, v_now, v_now),
        ('https://www.instagram.com/p/ext_cafe_m01_lo/', 'https://www.instagram.com/p/ext_cafe_m01_lo/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  200, 0, v_now, v_now),
        ('https://www.instagram.com/p/ext_act_er01_hi/', 'https://www.instagram.com/p/ext_act_er01_hi/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED', 1200, 0, v_now, v_now),
        ('https://www.instagram.com/p/ext_act_ps01_md/', 'https://www.instagram.com/p/ext_act_ps01_md/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  500, 0, v_now, v_now),
        ('https://www.instagram.com/p/ext_act_pk01_lo/', 'https://www.instagram.com/p/ext_act_pk01_lo/',
         'INSTAGRAM', 'DISPATCHED', 'SUCCEEDED',  150, 0, v_now, v_now)
    ON CONFLICT (normalized_url) DO NOTHING;

    -- room_links 삽입
    INSERT INTO room_links (room_id, link_id, created_at, updated_at)
        SELECT v_room_id, id, v_now, v_now FROM links
        WHERE normalized_url IN (
            'https://www.instagram.com/p/ext_food_k01_hi/',
            'https://www.instagram.com/p/ext_food_j01_md/',
            'https://www.instagram.com/p/ext_food_w01_lo/',
            'https://www.instagram.com/p/ext_cafe_bk01_hi/',
            'https://www.instagram.com/p/ext_cafe_cd01_md/',
            'https://www.instagram.com/p/ext_cafe_m01_lo/',
            'https://www.instagram.com/p/ext_act_er01_hi/',
            'https://www.instagram.com/p/ext_act_ps01_md/',
            'https://www.instagram.com/p/ext_act_pk01_lo/'
        )
    ON CONFLICT (room_id, link_id) DO NOTHING;

    -- origin_room_link_id 업데이트 (POPULAR 선정 기준이 됨)
    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_food_k01_hi/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_food_k01');

    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_food_j01_md/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_food_j01');

    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_food_w01_lo/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_food_w01');

    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_cafe_bk01_hi/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_cafe_bk01');

    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_cafe_cd01_md/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_cafe_cd01');

    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_cafe_m01_lo/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_cafe_m01');

    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_act_er01_hi/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_act_er01');

    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_act_ps01_md/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_act_ps01');

    UPDATE room_places SET origin_room_link_id = (
        SELECT rl.id FROM room_links rl JOIN links l ON rl.link_id = l.id
        WHERE rl.room_id = v_room_id AND l.normalized_url = 'https://www.instagram.com/p/ext_act_pk01_lo/'
    ) WHERE room_id = v_room_id AND place_id = (SELECT id FROM places WHERE kakao_place_id = 'ext_act_pk01');

    RAISE NOTICE '[seed-datecourse-test] 완료 — 장소 27개 / 영업시간 27개 / room_places 삽입 / links 9개(POPULAR용) 추가됨 (room: %)', v_room_pid;
END $$;
