# ADR-007: 트러블슈팅 관련 의사결정

| 작성일 | 수정일 |
|---|---|
| 2026-05-19 | 2026-05-19 |

---

## 1. 스케줄러 배치 처리의 트랜잭션 분리

**상태**: 결정됨

### 컨텍스트

만료된 PENDING 수강신청을 일괄 취소하는 스케줄러를 처음 구현할 때 전체 배치를 단일 `@Transactional`로 묶었다.

```java
// 문제가 있던 구조
@Transactional
public void cancelExpiredPendingEnrollments() {
    List<Enrollment> expired = enrollmentRepository.findExpiredPendingEnrollments(...);
    for (Enrollment enrollment : expired) {
        enrollment.expirePayment();
        klassRepository.increaseRemainingCapacity(klassId);
        waitlistEventPublisher.publish(klassId);
    }
}
```
전체 배치가 단일 트랜잭션으로 묶여 있기 때문에, N건 처리 중 중간에 하나가 실패하면 이전 건들의 DB 변경까지 전부 롤백된다.
결과적으로 정상 처리됐어야 할 건들도 전부 롤백되어 다음 스케줄러 실행까지 취소가 지연된다.

### 결정

스케줄러에서 **건별로 트랜잭션을 분리**해 호출한다.

```
스케줄러 (트랜잭션 없음)
  → 서비스: 만료된 수강신청 ID 목록 조회
  → for each id:
      try { 서비스: 단건 취소 처리 (@Transactional) }
      catch { 실패 로그, 다음 건 계속 처리 }
```

- 조회 메서드와 단건 처리 메서드를 분리한다.
- 스케줄러에서 건별로 서비스 메서드를 호출한다. 스케줄러 → 서비스는 Spring 프록시를 통한 호출이므로 `@Transactional`이 정상 적용된다.

> cf. 같은 서비스 클래스 안에서 단건 처리 메서드를 분리하고 루프에서 호출하는 방식은 동작하지 않는다. 왜?  
> 같은 클래스 내 호출은 Spring 프록시를 우회(self-invocation)하여 @Transactional이 적용되지 않기 때문이다.
> id만 받아온 뒤 스케줄러(다른 클래스)에서 호출을 반복하는 현재 방식은 이런 문제가 생기지 않는다.

### 이유

- 한 건의 실패가 다른 건에 영향을 주어서는 안 된다.
- 실패한 건은 다음 스케줄러 실행 시 자동으로 재처리된다. 별도 실패 큐 없이도 자연스러운 재시도가 보장된다.
- 엔티티 전체를 조회해 단건 트랜잭션으로 넘기면, 준영속 상태이므로 어차피 lazy 로딩을 쓸 수 없다. 
  따라서 ID만 넘기고 단건 처리 메서드 안에서 엔티티를 다시 조회해 lazy 로딩을 사용한다.
- lazy 로딩은 애플리케이션 코드 수준에서 N+1 쿼리가 발생하지만, 1시간 주기의 백그라운드 작업이고 처리 건수가 소규모이므로 용인할 수 있는 성능 저하로 판단했다.

<br>

`closeExpiredKlasses()`도 단일 트랜잭션 배치 구조이지만 별도로 분리하지 않았다. 루프 안에서 수행하는 작업이 `Klass::close()` — 순수 인메모리 필드 변경뿐이므로 중간에 실패할 여지가 없기 때문이다.
커밋 자체가 실패하는 경우에는 다음 스케줄러 실행 시 해당 강의들이 여전히 OPEN 상태로 남아 있어 자연스럽게 재처리된다.

> cf. 컨트롤러 → 서비스 흐름에서는 OSIV(`spring.jpa.open-in-view`)가 HTTP 요청 스레드에 EntityManager를 바인딩하여 트랜잭션 밖에서도 lazy 로딩이 가능하다.
> 그러나 (당연하게도) 스케줄러는 백그라운드 스레드에서 실행되므로 OSIV가 적용되지 않는다. `@Transactional`이 닫히면 EntityManager도 닫혀 준영속 상태가 되므로, 스케줄러에서는 트랜잭션 경계를 더 신경써야 한다.
