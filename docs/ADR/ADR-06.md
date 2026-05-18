# ADR-006: Repository 메서드 네이밍 컨벤션

| 작성일 | 수정일 |
|---|---|
| 2026-05-18 | 2026-05-18 |

---

**상태**: 결정됨

## 컨텍스트

Spring Data JPA는 메서드 이름으로 쿼리를 자동 생성하는 파생 쿼리(derived query) 기능을 제공한다.
필드명을 그대로 나열하는 방식이므로 레포지토리 선언부에서는 직관적이지만,
조건이 늘어날수록 서비스 호출부에서 메서드 이름이 길어지고 비즈니스 의도를 파악하기 어려워진다.

```java
// 서비스 호출부에서 읽어야 하는 코드
enrollmentRepository.existsByKlassmateIdAndKlassIdAndStatusNot(klassmateId, klassId, CANCELLED)
// → "CANCELLED가 아닌 것이 있으면 이미 활성 수강신청이 있다는 뜻"이라고 번역해야 함
```

서비스 코드를 읽는 빈도가 레포지토리 선언부를 읽는 빈도보다 높기 때문에,
서비스 호출부의 가독성과 직관성을 우선한다.

## 결정

| 조건                                                                | 네이밍 방식 |
|-------------------------------------------------------------------|---|
| 조건 2개 이하, 부정/집합 없음                                                | JPA 파생 쿼리 (`findBy...`, `existsBy...`) |
| 조건 3개 이상, 또는 `Not` / `In` / `Between` 포함, 비즈니스 로직을 강하게 포함하는 조회 쿼리 | 비즈니스 의미 기반 네이밍 + `@Query` |

### 파생 쿼리를 유지하는 경우

```java
// 조건 1개
Optional<Enrollment> findById(Long id);

// 조건 2개, 부정/집합 없음
Optional<Enrollment> findByIdAndKlassmateId(Long id, Long klassmateId);
boolean existsByKlassmateIdAndKlassId(Long klassmateId, Long klassId);
```

### `@Query`로 전환하는 경우

```java
// 조건 3개 + 부정(Not) → isAlreadyEnrolled
@Query("SELECT COUNT(e) > 0 FROM Enrollment e " +
       "WHERE e.klassmate.id = :klassmateId AND e.klass.id = :klassId " +
       "AND e.status <> 'CANCELLED'")
boolean isAlreadyEnrolled(@Param("klassmateId") Long klassmateId,
                          @Param("klassId") Long klassId);

// 집합(In) → findRegisteredEnrollmentsByKlassId
@Query("SELECT e FROM Enrollment e " +
       "WHERE e.klass.id = :klassId " +
       "AND e.status IN ('PENDING', 'CONFIRMED')")
List<Enrollment> findRegisteredEnrollmentsByKlassId(@Param("klassId") Long klassId);
```

## 이유

- 서비스 코드에서 메서드 이름만 읽어도 비즈니스 동작이 파악되어야 한다.
- 파생 쿼리는 구현 세부사항(필드명, 연산자)을 그대로 노출한다. 조건이 복잡해질수록 호출부에서 머릿속으로 번역하는 비용이 커진다.
- `@Query`를 추가하는 비용은 일회성이지만, 서비스 코드를 읽는 비용은 반복된다.
