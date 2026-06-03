# 에러 응답 계약

백엔드 오류 응답은 RFC 7807 `ProblemDetail` 형식을 사용한다.

## 기본 구조

```json
{
  "title": "E400_VALIDATION",
  "status": 400,
  "detail": "입력값을 확인해 주세요.",
  "instance": "/api/v1/rooms",
  "code": "E400_VALIDATION",
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

`code`는 백엔드 `ErrorCode` 이름과 동일하다. `fieldErrors`는 입력 필드 오류가 있을 때만 포함된다.

## 프론트 표시 기준

입력 필드 오류는 `fieldErrors`를 사용한다.

- `fieldErrors`가 있으면 각 `field`에 해당하는 input 아래에 `message`를 표시한다.
- 이때 `detail`은 `"입력값을 확인해 주세요."` 같은 일반 안내 문구이며, 필드별 실제 문구는 `fieldErrors[].message`를 우선한다.
- Request DTO의 Bean Validation 메시지는 한국어로 내려온다.

일반 오류는 `detail`을 사용한다.

- `fieldErrors`가 없으면 `detail`을 toast, alert, modal 등 일반 오류 UI에 표시한다.
- 리소스 없음, 권한 없음, 중복 상태, 외부 API 실패, 서버 오류는 `detail` 중심으로 내려온다.

## `fieldErrors`로 내려가는 오류

- `@Valid` Request DTO 검증 실패
- 사용자 입력 필드의 필수값 누락, blank, 길이 초과, 숫자 범위 오류
- enum, 날짜/시간, URL, path/query parameter 형식 오류
- 시작 일시가 종료 일시보다 늦거나 같은 경우
- 방 이름, 닉네임, 카테고리, 지역 코드, 장소 ID 목록 등 특정 input에 귀속 가능한 정책 위반

## `detail`로 내려가는 오류

- 존재하지 않는 리소스
- 인증 필요, 권한 없음, 접근 불가
- 이미 참여한 방, 이미 저장된 코스, 중복 생성 불가
- 현재 상태상 수행할 수 없는 요청
- 외부 API 실패
- 서버 내부 오류
- 특정 input 하나에 귀속하기 어려운 비즈니스 오류

## 예시

필드 오류:

```json
{
  "title": "E400_VALIDATION",
  "status": 400,
  "detail": "입력값을 확인해 주세요.",
  "code": "E400_VALIDATION",
  "fieldErrors": [
    {
      "field": "nickname",
      "message": "닉네임은 최대 10자까지 가능합니다.",
      "rejectedValue": "12345678901"
    }
  ]
}
```

비즈니스 오류:

```json
{
  "title": "E409_CONFLICT",
  "status": 409,
  "detail": "이미 참여한 방입니다.",
  "code": "E409_CONFLICT"
}
```
