# 에러 응답 계약

백엔드 오류 응답은 RFC 7807 `ProblemDetail` 형식을 사용한다.

## 기본 구조

```json
{
  "title": "ROOM_ALREADY_JOINED",
  "status": 409,
  "detail": "이미 참여한 방입니다.",
  "instance": "/api/v1/rooms/join",
  "code": "ROOM_ALREADY_JOINED",
  "timestamp": "2026-06-03T00:00:00Z",
  "fieldErrors": [
    {
      "field": "name",
      "message": "방 이름은 필수입니다.",
      "rejectedValue": null
    }
  ]
}
```

- `code`는 백엔드 `ErrorCode` 이름과 동일하다. **프론트 분기·문구 매핑의 기준 키**다.
- `detail`은 로그·디버그·미등록 code fallback 용도다. UX 문구의 source of truth가 아니다.
- `fieldErrors`는 입력 필드 오류가 있을 때만 포함된다.

## 프론트 표시 기준

| 우선순위 | 소스 | 용도 |
|---------|------|------|
| 1 | `fieldErrors[].message` | 폼 input 아래 |
| 2 | `code` → `ERROR_TEXT` | 토스트·모달·분기 |
| 3 | HTTP `status` 공통 fallback | 미등록 code |
| 4 | `detail` | 최후 fallback |
| 5 | generic fallback | unknown |

성공 응답의 `message`는 UI에 사용하지 않는다. 성공 토스트는 프론트 `text.ts`를 사용한다.

## `fieldErrors`로 내려가는 오류

- `@Valid` Request DTO 검증 실패
- 사용자 입력 필드의 필수값 누락, blank, 길이 초과, 숫자 범위 오류
- enum, 날짜/시간, URL, path/query parameter 형식 오류

## 도메인 `ErrorCode` (발췌)

### Room
- `ROOM_NOT_FOUND`, `ROOM_ACCESS_FORBIDDEN`, `ROOM_NOT_MEMBER`
- `ROOM_ALREADY_JOINED`, `ROOM_MEMBER_LIMIT_REACHED`

### Place
- `ROOM_PLACE_NOT_FOUND`, `ROOM_PLACE_USED_IN_DATE_COURSE`, `ROOM_PLACE_SAVE_CONFLICT`

### Course
- `DATE_COURSE_NOT_FOUND`, `DATE_COURSE_ALREADY_SAVED`, `E409_DUPLICATE_DATE_COURSE`
- `DATE_COURSE_FORBIDDEN_EDIT`, `DATE_COURSE_FORBIDDEN_DELETE`
- `DATE_COURSE_NO_PLACES`, `DATE_COURSE_GENERATION_EMPTY`

### Link
- `LINK_NOT_FOUND`, `LINK_ANALYSIS_REQUEST_NOT_FOUND`, `LINK_ANALYSIS_REQUEST_FORBIDDEN`
- `LINK_ANALYSIS_NOT_COMPLETED`, `LINK_ANALYSIS_RETRY_NOT_ALLOWED`, `LINK_ANALYSIS_NOT_EXPIRED`
- `LINK_ANALYSIS_RETRY_STATE_CHANGED`, `LINK_ANALYSIS_INSTAGRAM_COOLDOWN`

### User / Auth
- `USER_NOT_FOUND`, `ONBOARDING_ALREADY_COMPLETED`, `USER_ACCOUNT_DISABLED`
- `WEB_LOGIN_TICKET_INVALID`, `MOBILE_AUTH_CODE_INVALID`, `REFRESH_TOKEN_INACTIVE` 등

### 공통 HTTP (Spring Security 등)
- `E401_UNAUTHORIZED`, `E403_FORBIDDEN`, `E404_NOT_FOUND`, `E409_CONFLICT`, `E500_INTERNAL`

새 비즈니스 오류 추가 시 `E409_CONFLICT` 재사용 대신 도메인 code를 추가한다.

## 예시

비즈니스 오류:

```json
{
  "title": "LINK_ANALYSIS_RETRY_NOT_ALLOWED",
  "status": 409,
  "detail": "재시도할 수 없는 링크 분석 요청입니다.",
  "code": "LINK_ANALYSIS_RETRY_NOT_ALLOWED"
}
```
