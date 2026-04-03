# REST API 규칙 (v1)

## 버전 정책

- **공개 REST API**는 URI에 **`/api/v1`** 접두사를 둔다.
- 이후 호환을 깨는 변경이 필요하면 **`/api/v2`** 등 새 prefix로 API를 추가하고, v1은 일정 기간 유지·폐기 정책을 따른다.

## 경로 구분

| 구분 | 경로 예 | 용도 |
|------|---------|------|
| 버전 있는 공개 API | `/api/v1/...` | 클라이언트(웹·앱)가 호출하는 REST 엔드포인트 |
| SpringDoc / Swagger UI | `/v3/api-docs`, `/swagger-ui.html` | API 명세 (개발·연동용) |
| Actuator | `/actuator/...` | 헬스·메트릭 등 운영·오케스트레이션 (Kubernetes liveness/readiness 등) |

**Actuator는 `/api/v1`에 포함하지 않는다.** 인프라 프로브는 `/actuator/health/liveness`, `/actuator/health/readiness` 등을 사용한다.

## RESTful 가이드

- 리소스는 **복수형 명사** 위주로 표현한다 (`/api/v1/users`, `/api/v1/courses`).
- HTTP 메서드: 조회 `GET`, 생성 `POST`, 수정 `PUT` 또는 `PATCH`, 삭제 `DELETE`.
- 응답·에러 형식은 애플리케이션 전역 규칙(`ProblemDetail`, 공통 응답 래퍼 등)을 따른다.

## 현재 엔드포인트 예시

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/v1/health` | L7/클라이언트용 헬스(애플 정보·타임스탬프 등). 인프라용은 Actuator 사용 |

## 새 컨트롤러 추가 시

- `@RequestMapping` / 클래스 레벨 경로에 **`/api/v1`** 접두사를 포함한 뒤 리소스 경로를 붙인다.

```java
@RequestMapping("/api/v1/courses")
public class CourseController { ... }
```

인터페이스(SpringDoc)와 구현 클래스에 동일한 prefix를 맞춘다.
