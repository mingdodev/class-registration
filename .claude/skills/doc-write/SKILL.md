---
name: doc-write
description: >
  개발자가 설계한 내용을 읽기 쉬운 글로 문서화한다.
  API 명세, 도메인 문서, DB 스키마, 실행 방법, 기술 의사결정(ADR) 기록 등을 작성할 때 반드시 이 스킬을 사용한다.
  개발자가 "문서 만들어줘", "API 명세 작성해줘", "README 써줘", "기술 결정 기록해줘" 등의 요청을 하면 이 스킬을 트리거한다.
---

# Doc Write Skill

개발자가 설계한 내용을 그 의도를 살려서 간단 명료하게 문서화한다.
문서화에 꼭 필요한 내용은 개발자가 제시한다. 정보가 불충분하면 문서 작성 전에 역질문한다.

---

## 원칙

- 개발자의 설계 의도를 왜곡하지 않는다.
- 간결하고 명확하게 작성한다. 불필요한 설명이나 미사여구를 붙이지 않는다.
- 추측으로 내용을 채우지 않는다. 모르는 부분은 반드시 개발자에게 질문한다.
- 변수 네이밍은 명확하게 한다. 과도한 생략과 축약을 하지 않는다.

---

## 역질문 기준

아래 정보 중 파악할 수 없는 부분이 있다면 문서 작성 전에 질문한다. 한 번에 묶어서 질문하되, 꼭 필요한 것만 묻는다.

| 문서 유형 | 필수 확인 항목 |
|---|---|
| API 명세 | 엔드포인트, HTTP 메서드, 요청/응답 형식, 에러 코드 |
| 도메인 문서 | 엔티티 목록, 속성, 비즈니스 규칙 |
| DB 스키마 | 테이블 목록, 필드/타입/제약, 테이블 간 관계 |
| 실행 방법 | 환경 변수, 사전 조건, 실행 명령어, 프로파일 |
| 기술 의사결정 | 결정 내용, 선택 이유, 고려했던 대안, 트레이드오프 |

---

## 문서 유형별 작성 가이드

### 1. API 명세

- 인증은 `X-User-Id` 헤더로 처리한다.
- 공통 응답과 에러 응답 포맷은 문서 상단에 한 번만 정의한다.
- 복잡한 중첩 구조가 있는 경우에만 샘플 요청/응답을 추가한다.

파일 구조:
```markdown
    # API 명세서

    | 작성일 | 수정일 |
    |---|---|
    | YYYY-MM-DD | YYYY-MM-DD |

    ## 목차
    - [공통 응답 포맷](#공통-응답-포맷)
    - [페이지네이션 응답 포맷](#페이지네이션-응답-포맷)
    - [에러 응답 포맷](#에러-응답-포맷)
    - [도메인명 API](#도메인명-api)

    ---

    ## 공통 응답 포맷

    성공적인 API 응답은 다음 형식을 따릅니다.

    ```json
    {
      "success": true,
      "data": { },
      "message": "요청이 성공적으로 처리되었습니다."
    }
    ```
    
    ---
    
    ## 페이지네이션 응답 포맷
    
    페이지네이션이 필요한 조회 API는 다음 형식을 따릅니다.
    커서 기반 페이지네이션을 사용하며, 클라이언트가 size를 지정할 수 있습니다.
    첫 요청은 cursor 없이 보내고, 응답의 nextCursor를 다음 요청에 넘깁니다.
    
    요청:
    `GET /api/resource?cursor=2024-01-01T00:00:00.000Z&size=10`
    
    응답:
    ```json
    {
        "success": true,
        "data": {
            "content": [ ],
            "nextCursor": "2024-01-01T00:00:00.000Z",
            "hasNext": true
        },
        "message": "요청이 성공적으로 처리되었습니다."
    }
    ```
    - nextCursor: 마지막 항목의 createdAt 값
    - hasNext: false이면 다음 페이지 없음

    ---

    ## 에러 응답 포맷

    에러 발생 시 RFC 9457 표준을 따르는 ProblemDetail 포맷으로 응답합니다.

    ```json
    {
      "type": "https://api.example.com/errors/{error_code}",
      "title": "{HTTP Reason Phrase}",
      "status": 400,
      "detail": "{상세 설명}",
      "instance": "/api/...",
      "code": "{ERROR_CODE}",
      "timestamp": "{ISO-8601 UTC}"
    }
    ```

    ---

    ## [도메인명] API

    ### POST /api/resource

    리소스를 생성한다.

    #### 요청 헤더
    ```
    Content-Type: application/json
    X-User-Id: 1
    ```

    #### 요청 본문
    ```json
    {
      "field": "value"
    }
    ```

    #### 응답 본문 (201 Created)
    ```json
    {
      "success": true,
      "data": {
        "id": 1
      },
      "message": "..."
    }
    ```

    #### 에러 응답 (409 Conflict)
    ```json
    {
      "type": "https://api.example.com/errors/{error_code}",
      "title": "Conflict",
      "status": 409,
      "detail": "...",
      "instance": "/api/...",
      "code": "ERROR_CODE",
      "timestamp": "2025-01-01T00:00:00.000Z"
    }
    ```
```

---

### 2. 도메인 문서

비즈니스 관점에서 엔티티를 설명한다. 엔티티가 무엇인지, 어떤 규칙을 가지는지를 기술한다.

```markdown
## 사용자 (User)

(필요 시 설명) 서비스의 회원을 나타낸다.

### 속성
이름(name), 나이(age), 이메일(email)

### 규칙
- 사용자 아이디는 이메일이며 중복될 수 없다.
- 나이는 0 이상이어야 한다.
```

---

### 3. DB 스키마

DB 설계 관점에서 테이블 구조와 관계를 기술한다. 비즈니스 규칙은 작성하지 않는다.

```markdown
## users

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 식별자 |
| email | VARCHAR(255) | UNIQUE, NOT NULL | 로그인 이메일 |
| password | VARCHAR(255) | NOT NULL | 암호화된 비밀번호 |
| created_at | DATETIME | NOT NULL | 가입 시각 |

**관계**
- users 1 : N orders
```

---

### 4. 실행 방법

```markdown
## 실행 방법

### 사전 조건
- Java 21
- (필요 시 추가)

### 프로파일
디폴트 프로파일은 `local`이다. `application.yml`에 `spring.profiles.active=local`이 설정되어 있다.
다른 환경으로 실행하려면 아래와 같이 지정한다.

### 환경 변수

| 변수명 | 설명 | 예시 |
|---|---|---|
| DB_URL | 데이터베이스 접속 URL | jdbc:mysql://localhost:3306/mydb |

### 실행

```bash
# local (기본)
./gradlew bootRun

# 다른 프로파일 지정 시
./gradlew bootRun --args='--spring.profiles.active=stage'

# 테스트
./gradlew test
```
```

---

### 5. 기술 의사결정 (ADR)

```markdown
## ADR-001: [결정 제목]

**날짜**: YYYY-MM-DD
**상태**: 결정됨

### 컨텍스트
이 결정이 필요했던 배경과 문제 상황.

### 결정
무엇을 선택했는가.

### 이유
선택한 근거.

### 고려한 대안
| 대안 | 제외 이유 |
|---|---|
| 대안 A | 이유 |

### 결과
이 결정으로 인한 트레이드오프나 후속 영향.
```

---

## 출력 형식

- 마크다운 `.md` 파일로 작성한다.
- `docs/` 하위에 저장한다.
- 파일명은 문서 유형에 맞게 지정한다. 예: `README.md`, `API.md`, `DOMAIN.md`, `SCHEMA.md`, `ADR-001.md`
- `README.md`를 제외한 모든 문서는 문서 제목 하단에 최초 작성일과 마지막 수정일을 기록한다.

```markdown
| 작성일 | 수정일 |
|---|---|
| YYYY-MM-DD | YYYY-MM-DD |
```