# ADR-005: DTO 설계 의사결정

| 작성일 | 수정일 |
|---|---|
| 2026-05-18 | 2026-05-18 (섹션 5 추가) |

---

## 1. Web DTO를 서비스 레이어까지 직접 전달

**상태**: 결정됨

### 컨텍스트
Controller에서 받은 Request DTO를 Service에 그대로 전달할 경우, 서비스가 표현 계층에 의존하게 된다.
비즈니스 로직이 외부에 대해 알게 되므로 UI 요구사항이 변경될 경우, 이와 무관한 서비스 레이어의 코드를 변경해야 하는 문제가 생긴다.
이를 분리하려면 서비스 전용 Command 객체를 별도로 만들어야 한다.

### 결정
서비스 전용 Command 객체를 만들지 않고, Web DTO를 서비스 레이어까지 직접 전달한다.

### 이유
Command 객체 분리가 유효한 경우는 다음과 같다.

- UI 요구사항이 자주 변경될 때
- REST 외 다른 진입점(gRPC, Kafka 등)이 동일한 서비스를 호출할 때
- 서비스가 라이브러리로 배포될 때

이 프로젝트는 단일 REST API 진입점만 존재하며, 변경의 여지도 없으므로 분리의 이점이 거의 없다.
오히려 Command 객체를 추가하면 같은 데이터를 표현하는 클래스가 불필요하게 늘어난다.

---

## 2. DTO 네이밍 컨벤션

**상태**: 결정됨

### 결정

| 구분 | 컨벤션                       | 예시 |
|---|---------------------------|---|
| 목록 아이템 | `{Entity}SummaryResponse` | `KlassSummaryResponse` |
| 상세 조회 | `{Entity}DetailResponse`  | `KlassDetailResponse` |
| 목록 래퍼 (비페이지네이션) | `{Entity}ListResponse`    | `CreatorKlassListResponse` |
| 단일 액션 응답 | `{Entity}Response`        | `MyEnrollmentResponse` |
| 중첩 타입 | suffix 없음, 중첩 레코드를 생성해 사용 | `CreatorInfo`, `KlassSummary` |

### 이유
- 페이지네이션 여부는 `CursorPage<T>`가 컨텍스트를 이미 표현하므로 DTO 이름에 중복할 필요 없다.
- 중첩 타입은 독립적인 HTTP 응답이 아니라 데이터 구조이므로 `Response`라는 suffix는 혼동을 줄 수 있다.
- 중첩 타입으로 단독 클래스(레코드)를 사용할 경우 변경 시 영향을 받는 범위가 불명확해질 수 있다.

---

## 3. 입력 값 검증 분리

**상태**: 결정됨

### 결정
Bean Validation(`@NotBlank`, `@NotNull`)은 필드 존재 여부 검증에만 사용한다. 비즈니스 규칙 검증(길이 제한, 최솟값 등)은 도메인 레이어에서 `BusinessException`으로 처리한다.

### 이유
API 명세에 `KLASS_TITLE_TOO_LONG`, `KLASS_CAPACITY_INVALID` 같은 도메인 전용 에러 코드가 정의되어 있다. Bean Validation으로 처리하면 `INVALID_INPUT`이라는 범용 코드로 응답되어 명세와 불일치가 생긴다. 검증 실패의 의미가 다르므로 처리 위치도 분리한다.

| 검증 유형 | 처리 위치 | 에러 코드 |
|---|---|---|
| 필수 필드 누락 | Bean Validation | `INVALID_INPUT` |
| 비즈니스 규칙 위반 | 도메인 레이어 | 도메인 전용 코드 |

---

## 4. DTO 구현체로 Java Record 사용

**상태**: 결정됨

### 결정
모든 DTO(Request, Response 모두)를 Java Record로 구현한다.

### 이유
- DTO는 데이터를 전달하는 역할만 하므로 불변성이 적합하다.
- Record는 생성자, getter, equals, hashCode, toString을 자동 생성해 boilerplate를 줄인다.
- Spring Boot 3+ / Jackson 2.12+에서 Record의 JSON 직렬화·역직렬화가 기본 지원된다.
- Lombok 없이도 간결하게 표현할 수 있다.

---

## 5. Response DTO 변환 위치: 컨트롤러에서 수행

**상태**: 결정됨

### 결정
서비스 레이어의 조회 메서드는 도메인 모델을 반환한다. Response DTO 변환은 컨트롤러에서 `ResponseDto.from(domain)`으로 수행한다.
조회 메서드에 `@Transactional(readOnly = true)`를 붙이지 않는다.

### 이유

**도메인 순수성**
서비스가 Response DTO를 반환하면 도메인 레이어가 프레젠테이션 형식을 알게 된다. 서비스는 도메인 모델만 반환하고, 표현 형식 결정은 컨트롤러에 위임한다.
Spring Boot는 OSIV(`spring.jpa.open-in-view`)가 기본으로 활성화되어 있어, 서비스 트랜잭션이 종료된 이후 컨트롤러에서 lazy 로딩이 발생해도 `LazyInitializationException`이 발생하지 않는다.
해당 서비스는 성능이 중요한 경우를 제외하고서는, OSIV를 활용해 레이어 간 책임을 분리하는 것을 더 우선시한다.

**`@Transactional(readOnly = true)` 미사용 이유**

dirty checking 최적화나 단일 트랜잭션으로의 묶음 등 성능 이점은 이론적으로 존재하나, 실제 운영 환경에서 측정된 수치 없이 적용하면 트랜잭션 범위 확대에 따른 사이드 이펙트(커넥션 점유 시간, 예상치 못한 추가 쿼리 등)를 정확히 예측하기 어렵다.
DB Replication 환경처럼 readOnly 힌트가 명확한 이점을 갖는 상황이 아니라면, 성능 검증이 선행된 이후 도입하는 것이 적절하다.

### 결과

'읽기'를 명시하지 않게 되므로, 실수로 쓰기가 끼어들어도 런타임에 감지되지 않는다.
코드 리뷰와 테스트로 보완한다.