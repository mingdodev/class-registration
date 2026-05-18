# class-registration

수강 신청 시스템. Spring Boot 3 + Java 21 + JPA.

## 문서

| 문서 | 경로 | 참조 시점 |
|------|------|-----------|
| 도메인 규칙 | `docs/DOMAIN.md` | 비즈니스 로직 구현 전 |
| API 명세 | `docs/API.md` | 엔드포인트 구현 전 |
| DB 스키마 | `docs/SCHEMA.md` | 엔티티/마이그레이션 작업 전 |
| ADR-001 | `docs/ADR/ADR-01.md` | 도메인 설계 의사결정 |
| ADR-002 | `docs/ADR/ADR-02.md` | API 설계 의사결정 |
| ADR-003 | `docs/ADR/ADR-03.md` | 영속성 및 DB 환경 설정 |
| ADR-004 | `docs/ADR/ADR-04.md` | 동시성, 스케줄러, 대기열 처리 |
| ADR-005 | `docs/ADR/ADR-05.md` | DTO 설계 |
| ADR-006 | `docs/ADR/ADR-06.md` | Repository 메서드 네이밍 컨벤션 |
| ADR-007 | `docs/ADR/ADR-07.md` | 트러블슈팅 관련 의사결정 |

## 패키지 구조

```
domain/
  creator/       # 강사
    model/
    repository/
  klass/         # 강의
    controller/
    dto/
    model/
    repository/
    service/
  klassmate/     # 수강생
    model/
    repository/
  enrollment/    # 수강 신청
    client/      # PaymentClient 인터페이스 및 구현체
    controller/
    dto/
    model/
    repository/
    service/
  waitlist/      # 대기열
    dto/
    model/
    publisher/
    repository/
    service/
global/
  config/
  exception/     # ErrorCode, GlobalExceptionHandler
  response/      # ApiResponse, CursorPage
  scheduler/     # EnrollmentScheduler, KlassScheduler, WaitlistScheduler
```

## 주요 규칙

- API 구현 전 `docs/API.md` 확인 — 경로·HTTP 메서드·응답 포맷 준수
- DTO 설계는 ADR-005 기준 적용
- Repository 메서드 네이밍은 ADR-006 기준 적용
- `@Transactional(readOnly = true)`은 꼭 필요한 경우에만 사용함
