---
name: test-write
description: >
  Spring Boot + Java 21 + JUnit 5 환경에서 테스트 코드를 작성하는 스킬.
  서비스 유닛 테스트, API 통합 테스트, Repository 테스트를 작성할 때 반드시 이 스킬을 사용한다.
  개발자가 "테스트 작성해줘", "테스트 케이스 만들어줘", "이 기능 테스트해줘",
  "유닛 테스트", "통합 테스트" 등의 요청을 하면 이 스킬을 트리거한다.
---

# Test Write Skill

Spring Boot 4 + Java 21 + JUnit 5 기반으로 테스트 코드를 작성한다.
테스트 환경은 `src/test/resources/application.yml`로 분리된 h2 인메모리 데이터베이스를 사용한다.

---

## 원칙

- 요구사항 기반으로 테스트 케이스를 도출한다.
- 실패/예외 케이스와 엣지 케이스를 빠짐없이 검토한다.
- 테스트 스타일을 통일하고 가독성을 최우선으로 한다.
- 변수 네이밍은 명확하게 한다. 과도한 생략과 축약을 하지 않는다.
- 테스트 데이터는 Fixture 패턴을 사용해 외부 환경에 의존하지 않는다.

---

## 테스트 종류와 책임

| 종류 | 책임 | 작성 시점 |
|---|---|---|
| 서비스 유닛 테스트 | 비즈니스 규칙 검증 | 항상 작성 |
| API 통합 테스트 | 요청 → DB 반영까지 전체 흐름 검증 | 항상 작성 |
| Repository 테스트 | 복잡한 쿼리/트랜잭션 세부 동작 검증 | 트랜잭션 경계 검증이 필요할 때, 조인 결과가 비즈니스 의미를 가질 때 |

---

## 네이밍 규칙

### 서비스 유닛 테스트
```
[도메인 상황]_하면_[비즈니스 결과]가_발생한다
```

예시:
```java
@Test
void 이미_가입된_이메일로_회원가입_하면_예외가_발생한다() { ... }

@Test
void 유효한_정보로_회원가입_하면_사용자가_저장된다() { ... }
```

### API 통합 테스트
```
[요청 상황]_하면_[HTTP 상태코드]_를_응답하고_[부수효과]가_발생한다
```

예시:
```java
@Test
void 유효한_회원가입_요청_하면_201을_응답하고_사용자가_저장된다() { ... }

@Test
void 중복_이메일로_회원가입_요청_하면_409를_응답하고_사용자가_저장되지_않는다() { ... }

@Test
void 비밀번호_없이_회원가입_요청_하면_400을_응답하고_사용자가_저장되지_않는다() { ... }
```

---

## Fixture 패턴

테스트 데이터 생성은 Fixture 클래스로 중앙화한다. 테스트 코드에서 엔티티나 요청 객체를 직접 생성하지 않는다.

### 엔티티 Fixture

```java
public class UserFixture {

    public static User 일반_사용자() {
        return new User("user@example.com", "encodedPassword");
    }

    public static User 사용자(String email) {
        return new User(email, "encodedPassword");
    }
}
```

### 요청 빌더 Fixture

MockMvc에 넘길 요청 객체를 미리 정의해둔다.

```java
public class UserRequestFixture {

    public static UserCreateRequest 유효한_회원가입_요청() {
        return new UserCreateRequest("new@example.com", "password123");
    }

    public static UserCreateRequest 중복_이메일_회원가입_요청() {
        return new UserCreateRequest("existing@example.com", "password123");
    }
}
```

### 사용 예시

```java
@Test
void 유효한_회원가입_요청_하면_201을_응답하고_사용자가_저장된다() throws Exception {
    // given
    UserCreateRequest request = UserRequestFixture.유효한_회원가입_요청();

    // when & then
    mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
}
```

---

## 테스트 유형별 작성 가이드

### 1. 서비스 유닛 테스트

- MockRepository를 사용해 비즈니스 규칙만 빠르게 검증한다.
- DB, 네트워크 등 외부 의존성을 제거한다.
- 많이, 빠르게 작성한다.

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 이미_가입된_이메일로_회원가입_하면_예외가_발생한다() {
        // given
        UserCreateRequest request = UserRequestFixture.중복_이메일_회원가입_요청();
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void 유효한_정보로_회원가입_하면_사용자가_저장된다() {
        // given
        UserCreateRequest request = UserRequestFixture.유효한_회원가입_요청();
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        userService.createUser(request);

        // then
        then(userRepository).should().save(any(User.class));
    }
}
```

---

### 2. API 통합 테스트

- Controller → Service → Repository → DB 전체 흐름을 검증한다.
- 요청/응답 형식, 상태코드, validation, 헤더 인증 흐름, DB 반영까지 확인한다.
- `@SpringBootTest` + `@AutoConfigureMockMvc` 사용.
- 각 테스트는 독립적이어야 한다. `@Transactional` 또는 `@BeforeEach` 데이터 초기화로 격리한다.
- DB 검증은 **"의도한 부수효과가 발생했는가"** 까지만 한다. 저장 여부, 삭제 여부, 건수 변화 정도가 적절한 범위다. 조인 결과, 정렬 순서, 필드 세부값 검증은 Repository 테스트 영역이다.

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 유효한_회원가입_요청_하면_201을_응답하고_사용자가_저장된다() throws Exception {
        // given
        UserCreateRequest request = UserRequestFixture.유효한_회원가입_요청();

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(request.email()));

        assertThat(userRepository.findByEmail(request.email())).isPresent();
    }

    @Test
    void 중복_이메일로_회원가입_요청_하면_409를_응답하고_사용자가_저장되지_않는다() throws Exception {
        // given
        userRepository.save(UserFixture.일반_사용자());
        UserCreateRequest request = UserRequestFixture.중복_이메일_회원가입_요청();
        long userCountBefore = userRepository.count();

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }
}
```

---

### 3. Repository 테스트

트랜잭션 경계 검증이 필요할 때, 조인 결과가 비즈니스 의미를 가질 때 작성한다. 단순 CRUD는 작성하지 않는다.

```java
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void 특정_사용자의_완료된_주문만_최신순으로_조회된다() {
        // given
        User user = entityManager.persist(UserFixture.일반_사용자());
        entityManager.persist(new Order(user, OrderStatus.COMPLETED, LocalDateTime.now().minusDays(1)));
        entityManager.persist(new Order(user, OrderStatus.COMPLETED, LocalDateTime.now()));
        entityManager.persist(new Order(user, OrderStatus.CANCELLED, LocalDateTime.now()));
        entityManager.flush();

        // when
        List<Order> completedOrders = orderRepository.findCompletedOrdersByUserOrderByCreatedAtDesc(user.getId());

        // then
        assertThat(completedOrders).hasSize(2);
        assertThat(completedOrders.get(0).getCreatedAt())
                .isAfter(completedOrders.get(1).getCreatedAt());
    }
}
```

---

## 테스트 케이스 도출 체크리스트

테스트 작성 전 아래를 검토한다.

**정상 케이스**
- [ ] 가장 기본적인 성공 흐름
- [ ] 선택 파라미터가 있을 때와 없을 때

**실패/예외 케이스**
- [ ] 필수 입력값 누락
- [ ] 형식 오류 (타입, 길이, 패턴)
- [ ] 비즈니스 규칙 위반 (중복, 상태 불일치 등)
- [ ] 존재하지 않는 리소스 접근
- [ ] 권한 없는 접근 (헤더 인증이 있는 경우)

**엣지 케이스**
- [ ] 경계값 (최솟값, 최댓값)
- [ ] 빈 컬렉션 / null 허용 여부
- [ ] 동시성 이슈 가능성 (필요 시 별도 검토)