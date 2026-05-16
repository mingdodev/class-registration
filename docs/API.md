# API 명세서

| 작성일 | 수정일 |
|---|---|
| 2026-05-16 | 2026-05-17 |

## 목차
- [공통 응답 포맷](#공통-응답-포맷)
- [페이지네이션 응답 포맷](#페이지네이션-응답-포맷)
- [에러 응답 포맷](#에러-응답-포맷)
- [에러 코드](#에러-코드)

| 도메인 | 메서드 | 엔드포인트 | 설명 |
|---|---|---|---|
| 강의 | GET | [/api/klasses](#get-apiklasses) | 전체 강의 목록 조회 |
| 강의 | GET | [/api/klasses/{klassId}](#get-apiklassesklassid) | 강의 상세 조회 |
| 강의 | PATCH | [/api/klasses/{klassId}](#patch-apiklassesklassid) | 강의 수정 |
| 강의 | DELETE | [/api/klasses/{klassId}](#delete-apiklassesklassid) | 강의 삭제 |
| 강사 | POST | [/api/klasses](#post-apiklasses) | 강의 초안 등록 |
| 강사 | PATCH | [/api/klasses/{klassId}/open](#patch-apiklassesklassidopen) | 강의 모집 시작 |
| 강사 | GET | [/api/creators/me/klasses](#get-apicreatorsmeklasses) | 내 강의 목록 조회 |
| 강사 | GET | [/api/klasses/{klassId}/klassmates](#get-apiklassesklassidklassmates) | 강의별 수강생 목록 조회 |
| 수강 신청 | GET | [/api/klasses/{klassId}/enrollments/me](#get-apiklassesklassidenrollmentsme) | 수강 신청 여부 확인 |
| 수강 신청 | POST | [/api/klasses/{klassId}/enrollments](#post-apiklassesklassidenrollments) | 수강 신청 |
| 수강 신청 | POST | [/api/enrollments/{enrollmentId}/confirm](#post-apienrollmentsenrollmentidconfirm) | 결제 완료 (수강 확정) |
| 수강 신청 | DELETE | [/api/enrollments/{enrollmentId}](#delete-apienrollmentsenrollmentid) | 수강 취소 |
| 수강 신청 | GET | [/api/klassmates/me/enrollments](#get-apiklassmatesmoenrollments) | 내 수강 신청 목록 조회 |
| 대기열 | POST | [/api/klasses/{klassId}/waitlist](#post-apiklassesklassidwaitlist) | 대기열 등록 |
| 대기열 | GET | [/api/klasses/{klassId}/waitlist/me](#get-apiklassesklassidwaitlistme) | 대기열 등록 여부 확인 |
| 대기열 | DELETE | [/api/klasses/{klassId}/waitlist](#delete-apiklassesklassidwaitlist) | 대기열 등록 취소 |

---

## 공통 응답 포맷

```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공적으로 처리되었습니다."
}
```

---

## 페이지네이션 응답 포맷

커서 기반 페이지네이션을 사용한다. 첫 요청은 `cursor` 없이 보내고, 응답의 `nextCursor`를 다음 요청에 넘긴다. 페이지 크기는 `size`로 지정하며 기본값은 10이다.

요청: `GET /api/resource?cursor=2026-05-16T00:00:00.000Z&size=10`

```json
{
  "success": true,
  "data": {
    "content": [],
    "nextCursor": "2026-05-10T14:30:00.000Z",
    "hasNext": true
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

> `nextCursor`: 마지막 항목의 `createdAt` 값
>
> `hasNext`: `false`이면 다음 페이지 없음

---

## 에러 응답 포맷

RFC 9457 표준을 따르는 ProblemDetail 포맷으로 응답합니다.

```json
{
  "type": "https://api.class-registration.com/errors/{error-code}",
  "title": "{HTTP Reason Phrase}",
  "status": 400,
  "detail": "{상세 설명}",
  "instance": "/api/...",
  "code": "{ERROR_CODE}",
  "timestamp": "2026-05-16T00:00:00.000Z"
}
```

---

## 에러 코드

### 강의 (Klass)

| 코드 | HTTP | 설명 |
|---|---|---|
| `KLASS_NOT_FOUND` | 404 | 강의를 찾을 수 없음 |
| `KLASS_TITLE_TOO_LONG` | 400 | 강의명이 20자를 초과함 |
| `KLASS_CAPACITY_INVALID` | 400 | 수강 정원이 1명 미만 |
| `KLASS_NOT_DRAFT` | 409 | DRAFT 상태가 아닌 강의를 모집 시작하려 함 |
| `KLASS_NOT_OPEN` | 409 | OPEN 상태가 아닌 강의에 수강 신청하려 함 |
| `KLASS_FULL` | 409 | 수강 정원이 가득 참 |
| `KLASS_PERIOD_ENDED` | 409 | 수강 기간이 종료됨 |
| `KLASS_ACCESS_DENIED` | 403 | 자신의 강의가 아님 |
| `KLASS_NOT_DELETABLE` | 409 | DRAFT 상태가 아닌 강의를 삭제하려 함 |
| `KLASS_CAPACITY_DECREASE_NOT_ALLOWED` | 400 | OPEN/CLOSED 상태에서 최대 수강 정원을 줄이려 함 |
| `KLASS_PRICE_UPDATE_NOT_ALLOWED` | 400 | OPEN/CLOSED 상태에서 가격을 수정하려 함 |

### 수강 신청 (Enrollment)

| 코드 | HTTP | 설명 |
|---|---|---|
| `ENROLLMENT_NOT_FOUND` | 404 | 수강 신청을 찾을 수 없음 |
| `ENROLLMENT_ALREADY_EXISTS` | 409 | 이미 수강 신청한 강의 (PENDING 또는 CONFIRMED 상태인 경우, CANCELLED 후 재신청은 가능) |
| `ENROLLMENT_NOT_PENDING` | 409 | PENDING 상태가 아닌 수강 신청을 결제 처리하려 함 |
| `ENROLLMENT_CANCEL_NOT_ALLOWED` | 409 | 취소 가능 기간(강의 시작일 3일 전)이 지남 |
| `ENROLLMENT_NOT_CONFIRMED` | 409 | CONFIRMED 상태가 아닌 수강 신청을 취소하려 함 |

### 대기열 (Waitlist)

| 코드 | HTTP | 설명 |
|---|---|---|
| `WAITLIST_NOT_AVAILABLE` | 409 | 대기열 등록 불가 (강의가 CLOSED 상태가 아니거나 수강 종료일이 지남) |
| `WAITLIST_ALREADY_EXISTS` | 409 | 이미 대기열에 등록된 수강생 |
| `WAITLIST_NOT_FOUND` | 404 | 대기열에 등록되어 있지 않음 |

---

## 강의 API

공통 헤더가 없는 경우 인증 없이 접근 가능합니다.

### GET /api/klasses

전체 강의 목록을 최신순으로 조회한다. OPEN, CLOSED 상태의 강의만 반환한다.

#### 쿼리 파라미터

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `status` | N | 강의 상태 필터 (`OPEN`, `CLOSED`) |
| `cursor` | N | 이전 응답의 `nextCursor` 값 (첫 요청 시 생략) |
| `size` | N | 페이지 크기 (기본값: 10) |

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "강의 제목",
        "price": 50000,
        "status": "OPEN",
        "remainingCapacity": 5,
        "startDate": "2026-06-01",
        "endDate": "2026-08-31"
      }
    ],
    "nextCursor": "2026-05-10T14:30:00.000Z",
    "hasNext": true
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

> `startDate`, `endDate`는 수강 기간이 무제한인 경우 `null`로 반환된다.

---

### GET /api/klasses/{klassId}

강의 상세 정보를 조회한다. 현재 수강 신청 인원을 포함한다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "강의 제목",
    "description": "강의 설명",
    "creator": {
      "id": 1,
      "name": "강사 이름"
    },
    "price": 50000,
    "status": "OPEN",
    "maxCapacity": 20,
    "remainingCapacity": 5,
    "enrolledCount": 15,
    "startDate": "2026-06-01",
    "endDate": "2026-08-31"
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

> `enrolledCount`는 `maxCapacity - remainingCapacity`로 산출된다.

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |

---

## 강사 API

강사 전용 API입니다. 모든 요청에 `X-Creator-Id` 헤더가 필요합니다.

```
X-Creator-Id: {강사 ID}
```

---

### POST /api/klasses

강의 초안(DRAFT)을 등록한다.

#### 요청 본문

```json
{
  "title": "강의 제목",
  "description": "강의 설명",
  "price": 50000,
  "maxCapacity": 20,
  "startDate": "2026-06-01",
  "endDate": "2026-08-31"
}
```

> `startDate`, `endDate`는 선택 항목이다. 둘 다 입력하거나 둘 다 생략해야 한다. 생략 시 수강 기간이 무제한으로 설정된다.

#### 응답 본문 (201 Created)

```json
{
  "success": true,
  "data": {
    "id": 1
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의명 20자 초과 | `KLASS_TITLE_TOO_LONG` | 400 |
| 수강 정원 1명 미만 | `KLASS_CAPACITY_INVALID` | 400 |

---

### PATCH /api/klasses/{klassId}/open

강의 모집을 시작한다. DRAFT 상태의 강의만 OPEN으로 전환할 수 있다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": null,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |
| 자신의 강의가 아님 | `KLASS_ACCESS_DENIED` | 403 |
| DRAFT 상태가 아님 | `KLASS_NOT_DRAFT` | 409 |

---

### PATCH /api/klasses/{klassId}

강의 정보를 수정한다. 수정 가능한 필드는 강의 상태에 따라 다르다.

| 필드 | DRAFT | OPEN / CLOSED |
|---|---|---|
| `title` | O | O |
| `description` | O | O |
| `price` | O | X |
| `maxCapacity` | O | O (증가만 가능) |
| `startDate` | O | O |
| `endDate` | O | O |

> `startDate`와 `endDate`는 함께 입력하거나 둘 다 생략해야 한다.

#### 요청 본문

```json
{
  "title": "수정된 강의 제목",
  "description": "수정된 강의 설명",
  "price": 60000,
  "maxCapacity": 30,
  "startDate": "2026-06-01",
  "endDate": "2026-09-30"
}
```

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": null,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |
| 자신의 강의가 아님 | `KLASS_ACCESS_DENIED` | 403 |
| 강의명 20자 초과 | `KLASS_TITLE_TOO_LONG` | 400 |
| OPEN/CLOSED 상태에서 가격 수정 | `KLASS_PRICE_UPDATE_NOT_ALLOWED` | 400 |
| OPEN/CLOSED 상태에서 정원 감소 | `KLASS_CAPACITY_DECREASE_NOT_ALLOWED` | 400 |

---

### DELETE /api/klasses/{klassId}

강의를 삭제한다. DRAFT 상태의 강의만 삭제할 수 있다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": null,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |
| 자신의 강의가 아님 | `KLASS_ACCESS_DENIED` | 403 |
| DRAFT 상태가 아님 | `KLASS_NOT_DELETABLE` | 409 |

---

### GET /api/creators/me/klasses

강사 자신이 개설한 강의 목록을 최신순으로 조회한다. DRAFT 포함 전체 상태를 조회할 수 있다.

#### 쿼리 파라미터

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `status` | N | 강의 상태 필터 (`DRAFT`, `OPEN`, `CLOSED`) |

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": {
    "klasses": [
      {
        "id": 1,
        "title": "강의 제목",
        "price": 50000,
        "status": "DRAFT",
        "remainingCapacity": 20,
        "startDate": "2026-06-01",
        "endDate": "2026-08-31"
      }
    ]
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

---

### GET /api/klasses/{klassId}/klassmates

강사 자신의 강의에 수강 신청한 수강생 목록을 조회한다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": {
    "klassmates": [
      {
        "id": 1,
        "name": "수강생 이름",
        "email": "klassmate@example.com",
        "phoneNumber": "010-1234-5678",
        "enrollmentStatus": "CONFIRMED",
        "enrolledAt": "2026-05-10T14:30:00.000Z"
      }
    ]
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |
| 자신의 강의가 아님 | `KLASS_ACCESS_DENIED` | 403 |

---

## 수강 신청 API

수강생 전용 API입니다. 모든 요청에 `X-Klassmate-Id` 헤더가 필요합니다.

```
X-Klassmate-Id: {수강생 ID}
```

---

### GET /api/klasses/{klassId}/enrollments/me

해당 강의에 대한 본인의 수강 신청 여부와 현재 상태를 확인한다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": {
    "enrolled": true,
    "enrollmentId": 42,
    "status": "CONFIRMED"
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

> 수강 신청 이력이 없는 경우 `enrolled: false`, `enrollmentId: null`, `status: null`로 반환된다.

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |

---

### POST /api/klasses/{klassId}/enrollments

강의를 수강 신청한다. 신청 직후 PENDING 상태가 된다.

#### 응답 본문 (201 Created)

```json
{
  "success": true,
  "data": {
    "enrollmentId": 42
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |
| OPEN 상태가 아님 | `KLASS_NOT_OPEN` | 409 |
| 정원이 가득 참 | `KLASS_FULL` | 409 |
| 수강 기간 종료 | `KLASS_PERIOD_ENDED` | 409 |
| 이미 수강 신청함 | `ENROLLMENT_ALREADY_EXISTS` | 409 |

---

### POST /api/enrollments/{enrollmentId}/confirm

결제를 완료하고 수강을 확정한다. PENDING 상태의 수강 신청만 처리할 수 있으며, 신청 후 24시간 이내에만 유효하다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": null,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 수강 신청 없음 | `ENROLLMENT_NOT_FOUND` | 404 |
| PENDING 상태가 아님 | `ENROLLMENT_NOT_PENDING` | 409 |

---

### DELETE /api/enrollments/{enrollmentId}

수강을 취소한다. CONFIRMED 상태이고 강의 시작일 3일 전까지만 취소할 수 있다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": null,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 수강 신청 없음 | `ENROLLMENT_NOT_FOUND` | 404 |
| CONFIRMED 상태가 아님 | `ENROLLMENT_NOT_CONFIRMED` | 409 |
| 취소 가능 기간 초과 | `ENROLLMENT_CANCEL_NOT_ALLOWED` | 409 |

---

### GET /api/klassmates/me/enrollments

수강생 자신의 수강 신청 목록을 최신순으로 조회한다.

#### 쿼리 파라미터

| 파라미터 | 필수 | 설명 |
|---|---|---|
| `status` | N | 수강 신청 상태 필터 (`PENDING`, `CONFIRMED`, `CANCELLED`) |
| `cursor` | N | 이전 응답의 `nextCursor` 값 (첫 요청 시 생략) |
| `size` | N | 페이지 크기 (기본값: 10) |

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 42,
        "klass": {
          "id": 1,
          "title": "강의 제목",
          "startDate": "2026-06-01",
          "endDate": "2026-08-31"
        },
        "status": "CANCELLED",
        "cancelReason": "PAYMENT_TIMEOUT",
        "createdAt": "2026-05-10T14:30:00.000Z",
        "updatedAt": "2026-05-11T14:30:00.000Z"
      }
    ],
    "nextCursor": "2026-05-10T14:30:00.000Z",
    "hasNext": false
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

> `cancelReason`은 `status`가 `CANCELLED`인 경우에만 포함된다.
>
> `cancelReason` 값: `PAYMENT_TIMEOUT`(결제 기한 지남), `USER_REQUESTED`(직접 취소)

---

## 대기열 API

수강생 전용 API입니다. 모든 요청에 `X-Klassmate-Id` 헤더가 필요합니다.

```
X-Klassmate-Id: {수강생 ID}
```

---

### POST /api/klasses/{klassId}/waitlist

대기열에 등록한다. 강의가 CLOSED 상태이고 수강 종료일이 지나지 않은 경우에만 가능하다.

신청 가능 인원이 0에서 1 이상으로 바뀌면(수강 신청 취소 또는 수강 정원 증가) 대기열의 1순위 수강생에게 PENDING 상태의 수강 신청이 자동 생성된다.

#### 응답 본문 (201 Created)

```json
{
  "success": true,
  "data": null,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |
| 대기열 등록 불가 (CLOSED 아님 또는 수강 기간 종료) | `WAITLIST_NOT_AVAILABLE` | 409 |
| 이미 대기열에 등록됨 | `WAITLIST_ALREADY_EXISTS` | 409 |

---

### GET /api/klasses/{klassId}/waitlist/me

수강생 본인의 대기열 등록 여부를 확인한다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": {
    "registered": true,
    "createdAt": "2026-05-10T14:30:00.000Z"
  },
  "message": "요청이 성공적으로 처리되었습니다."
}
```

> 대기열에 등록되어 있지 않은 경우 `registered: false`, `createdAt: null`로 반환된다.

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |

---

### DELETE /api/klasses/{klassId}/waitlist

대기열 등록을 취소한다.

#### 응답 본문 (200 OK)

```json
{
  "success": true,
  "data": null,
  "message": "요청이 성공적으로 처리되었습니다."
}
```

#### 에러 응답

| 상황 | 코드 | HTTP |
|---|---|---|
| 강의 없음 | `KLASS_NOT_FOUND` | 404 |
| 대기열에 등록되어 있지 않음 | `WAITLIST_NOT_FOUND` | 404 |
