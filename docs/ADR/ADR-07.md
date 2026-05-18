# ADR-007: 트러블슈팅 관련 의사결정

| 작성일 | 수정일        |
|---|------------|
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

---

## 2. 강의 정원 수정의 동시성 처리

**상태**: 결정됨

### 컨텍스트

`updateKlass()`는 JPA 더티 체킹으로 `maxCapacity`와 `remainingCapacity`를 수정하는 read-modify-write 구조였다.

```java
// 기존 구조 — Klass.update() 내부
this.remainingCapacity += maxCapacity - this.maxCapacity;
this.maxCapacity = maxCapacity;
```

반면 수강 신청의 `decreaseRemainingCapacity()`는 원자적 DB 쿼리로 처리한다.

```java
UPDATE Klass k SET k.remainingCapacity = k.remainingCapacity - 1
WHERE k.id = :klassId AND k.remainingCapacity > 0
```

두 연산이 같은 행의 `remainingCapacity`를 서로 다른 방식으로 건드리기 때문에, 수강 신청과 정원 수정이 동시에 들어오면 Lost Update가 발생할 수 있다.

### 결정

정원 수정을 수강 신청과 동일하게 **원자적 DB 쿼리**로 처리한다.

```java
@Modifying(clearAutomatically = true)
@Query("""
    UPDATE Klass k
    SET k.maxCapacity = :newMaxCapacity,
        k.remainingCapacity = k.remainingCapacity + (:newMaxCapacity - k.maxCapacity)
    WHERE k.id = :klassId
""")
int updateCapacity(@Param("klassId") Long klassId, @Param("newMaxCapacity") int newMaxCapacity);
```

`Klass.update()`에서 정원 필드 대입을 제거하고 검증만 수행한다. 실제 DB 반영은 `KlassService`에서 `updateCapacity()`를 호출해 처리한다.

부가적으로, 원자적 쿼리로 전환하면서 강사가 실수로 정원 수정 요청을 동시에 두 번 보내는 경우의 중복 요청 경쟁도 함께 제거되었다.

### 이유

처음에는 강사의 중복 요청 문제만 보고 낙관적 락(`@Version`) 적용을 검토했다. 그러나 수강 신청(`decreaseRemainingCapacity`)이 이미 원자적 쿼리로 같은 행을 건드리고 있다는 점을 발견했다.
낙관적 락은 정원 수정 간의 충돌만 막을 뿐, 수강 신청과 정원 수정 사이의 경쟁은 해결하지 못한다.

두 연산을 모두 원자적 쿼리로 통일하면 DB가 행 단위로 직렬화하므로 모든 경쟁이 사라진다.

---

## 3. 대기열 이벤트 발행 타이밍과 트랜잭션 롤백 문제

**상태**: 미구현 개선 사항

### 컨텍스트

수강 신청 취소나 정원 증가 시, 서비스 레이어는 트랜잭션이 커밋되기 전에 `waitlistEventPublisher.publish()`를 호출한다.

```
취소 트랜잭션 내부:
  1. enrollment.cancel()
  2. klassRepository.increaseRemainingCapacity()
  3. waitlistEventPublisher.publish()  ← 아직 커밋 전
  4. 트랜잭션 커밋
```

이벤트를 수신한 `WaitlistEventQueue`는 즉시 `WaitlistProcessorService.process()`를 가상 스레드에서 실행한다. 이 처리는 별도 트랜잭션이므로, 원본 취소 트랜잭션이 롤백되더라도 대기열 처리는 이미 진행 중일 수 있다.

`WaitlistProcessorService.process()`는 `decreaseRemainingCapacity()`가 0을 반환하면 즉시 `false`를 리턴하고, 소비 로직에서 `queue.poll()`을 호출하지 않아 대기열 항목을 유지한다.
롤백으로 인해 실제로 재고가 늘지 않은 경우 `decreaseRemainingCapacity()`가 실패하므로 수강신청이 잘못 생성되는 상황은 방지된다.

### 현재 동작의 문제

`false`를 반환하면 소비 로직이 재시도를 수행하고, 재시도도 실패하면 `circuit.recordFailure()`가 호출된다.
이 경우는 롤백으로 인한 일시적 상태이므로 서킷 브레이커가 불필요하게 차단될 수 있다.

### 미구현 개선 방향

근본 해결책은 트랜잭션 커밋이 성공한 후에만 이벤트를 발행하는 것이다.

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleWaitlistEvent(WaitlistEvent event) {
    waitlistEventPublisher.publish(event.klassId());
}
```

`@TransactionalEventListener`를 사용하면 커밋 전 롤백 시 이벤트 자체가 발행되지 않아 불필요한 재시도와 서킷 차단이 사라진다.

현재는 로직의 정합성(잘못된 수강신청 생성 방지)은 보장되며, 불필요한 서킷 차단이 발생할 수 있다는 점을 인지한 상태로 과제 구현 범위 외로 남긴다.