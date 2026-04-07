# Test Scenarios — NovaLMS
**Date:** 2026-04-04
**Project:** NovaLMS — Spring Boot 3.3.4, Java 17, Spring Data JPA, Thymeleaf

---

## Tổng quan

Bao gồm **~300 test scenarios** cover đầy đủ **7 features** từ 14 documents spec/plan 2026-04-04.

---

## Cấu trúc test

```
src/test/java/com/example/DoAn/
├── DoAnApplicationTests.java                   ← hiện có (context load)
├── BaseIntegrationTest.java                   ← TẠO MỚI (shared setup)
├── BaseControllerTest.java                    ← TẠO MỚI (authenticated tests)
│
├── service/impl/
│   ├── ExpertAssignmentServiceTest.java       ← SPEC 001
│   ├── NotificationServiceTest.java            ← SPEC 003
│   ├── ExpertReviewServiceTest.java           ← SPEC 003
│   ├── WizardValidationServiceTest.java       ← SPEC Wizard
│   ├── QuestionGroupWizardServiceImplTest.java ← SPEC Wizard
│   ├── StudentAssignmentServiceTest.java       ← SPEC 004
│   └── TeacherAssignmentGradingServiceTest.java ← SPEC 005
│
├── controller/
│   ├── ExpertAssignmentControllerTest.java   ← SPEC 001
│   ├── ExpertReviewControllerTest.java        ← SPEC 003
│   ├── NotificationControllerTest.java         ← SPEC 003
│   ├── TeacherLessonQuizControllerTest.java    ← SPEC 002
│   ├── StudentAssignmentApiControllerTest.java ← SPEC 004
│   ├── TeacherAssignmentGradingControllerTest.java ← SPEC 005
│   ├── TeacherQuizGradingApiControllerTest.java ← SPEC 006
│   └── ExpertQuestionGroupWizardControllerTest.java ← SPEC Wizard
│
└── integration/
    ├── FullAssignmentFlowTest.java            ← SPEC 001 + 004
    ├── StudentAssignmentFlowTest.java          ← SPEC 004
    ├── TeacherAssignmentGradingFlowTest.java    ← SPEC 005
    ├── ExpertApprovalFlowTest.java            ← SPEC 003
    └── QuestionGroupWizardFlowTest.java       ← SPEC Wizard
```

---

## Test Data Setup (Base Classes)

### BaseIntegrationTest.java

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected EntityManager em;

    protected User expert, teacher, teacherB, student;

    @BeforeAll
    void setupUsers() {
        // Create test users with roles
        // EXPERT, TEACHER, STUDENT via test repositories
    }

    protected String expertToken() { ... }
    protected String teacherToken() { ... }
    protected String studentToken() { ... }

    // Flush + clear to ensure persisted data visible
    protected void persistAndClear(Object... entities) { ... }
}
```

### BaseControllerTest.java

```java
public abstract class BaseControllerTest extends BaseIntegrationTest {

    protected String authHeader(String token) {
        return "Authorization: Bearer " + token;
    }

    protected ResultActions authenticated(String token, MockHttpServletRequestBuilder builder) {
        return mockMvc.perform(builder.header("Authorization", "Bearer " + token));
    }

    protected ResultActions jsonPost(String url, Object body, String token) { ... }
    protected ResultActions jsonGet(String url, String token) { ... }
    protected ResultActions jsonPatch(String url, Object body, String token) { ... }
    protected ResultActions jsonDelete(String url, String token) { ... }
}
```

---

## Test Fixtures mẫu

### ExpertAssignmentServiceTest.java

```java
private User createExpertUser() {
    User u = new User();
    u.setEmail("expert@nova.com");
    u.setRole(testRole("EXPERT"));
    return userRepository.save(u);
}

private Quiz createDraftAssignment(User expert) {
    Quiz q = new Quiz();
    q.setTitle("Test Assignment");
    q.setQuizCategory("COURSE_ASSIGNMENT");
    q.setUser(expert);
    q.setStatus("DRAFT");
    q.setIsSequential(true);
    q.setSkillOrder("[\"LISTENING\",\"READING\",\"SPEAKING\",\"WRITING\"]");
    return quizRepository.save(q);
}

private Question createPublishedQuestion(String skill) {
    Question q = new Question();
    q.setContent("Test question for " + skill);
    q.setSkill(skill);
    q.setCefrLevel("B1");
    q.setQuestionType("MULTIPLE_CHOICE_SINGLE");
    q.setStatus("PUBLISHED");
    q.setSource("EXPERT_BANK");
    q.setUser(expert);
    return questionRepository.save(q);
}

private QuizRequestDTO createValidAssignmentDTO() {
    QuizRequestDTO dto = new QuizRequestDTO();
    dto.setTitle("Midterm Exam");
    dto.setQuizCategory("COURSE_ASSIGNMENT");
    dto.setPassScore(BigDecimal.valueOf(70));
    dto.setMaxAttempts(2);
    dto.setTimeLimitPerSkill(Map.of("SPEAKING", 2, "WRITING", 30));
    return dto;
}
```

### StudentAssignmentServiceTest.java

```java
private AssignmentSession createInProgressSession(Quiz quiz, User student) {
    AssignmentSession s = new AssignmentSession();
    s.setQuiz(quiz);
    s.setUser(student);
    s.setStatus("IN_PROGRESS");
    s.setCurrentSkillIndex(0);
    s.setSectionStatuses(
        "{\"LISTENING\":\"IN_PROGRESS\",\"READING\":\"LOCKED\",\"SPEAKING\":\"LOCKED\",\"WRITING\":\"LOCKED\"}");
    s.setSectionAnswers("{}");
    s.setStartedAt(LocalDateTime.now());
    return sessionRepository.save(s);
}
```

---

## Authentication Strategy

Dùng `@WithMockUser` hoặc JWT token trong MockMvc:

```java
// Option A: Mock User (đơn giản, nhanh)
@WithMockUser(roles = "EXPERT")
@Test
void testExpertEndpoint() { ... }

// Option B: JWT Token (chính xác hơn)
@Test
void testWithJwtToken() {
    String token = jwtUtil.generateToken(expert.getEmail());
    mockMvc.perform(get("/api/v1/expert/assignments")
        .header("Authorization", "Bearer " + token));
}
```

---

## Database Setup cho Integration Tests

### application-test.properties

```properties
# src/test/resources/application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
```

### pom.xml — thêm H2

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

**Lưu ý:** H2 có LIMIT không tương thích với MySQL 8.0 syntax.
Nếu gặp lỗi, dùng `@ActiveProfiles("test")` với MySQL test container
hoặc dùng `Flyway` để migrate schema.

---

## Coverage Summary

| Feature | Spec | Unit Tests | Integration Tests | Total |
|---|---|---|---|---|
| Expert tạo Assignment | SPEC 001 | 14 | 13 | 27 |
| Teacher tạo Lesson Quiz | SPEC 002 | 7 | 11 | 18 |
| Expert duyệt câu hỏi + Notification | SPEC 003 | 20 | 13 | 33 |
| Student làm Assignment | SPEC 004 | 19 | 9 | 28 |
| Teacher chấm Assignment | SPEC 005 | 11 | 7 | 18 |
| Teacher chấm Lesson Quiz | SPEC 006 | 15 | 6 | 21 |
| QuestionGroup Wizard | SPEC Wizard | 40 | 10 | 50 |
| **TỔNG** | | **126** | **69** | **195** |

---

## Running Tests

```bash
# Run all tests
cd NovaLMS/DoAn
./mvnw test

# Run specific test class
./mvnw test -Dtest=WizardValidationServiceTest

# Run specific test
./mvnw test -Dtest=WizardValidationServiceTest#TC-VAL-GRP-001

# Run integration tests only
./mvnw test -Dtest="*IntegrationTest,*FlowTest"

# Run service layer only
./mvnw test -Dtest="*ServiceTest,*ServiceImplTest"

# With coverage report
./mvnw test jacoco:report
# View: target/site/jacoco/index.html
```

---

## Test Data Strategy

### 1. Pure Unit Tests (Mocked)
- Không cần DB
- Mock repository → kiểm tra logic nghiệp vụ
- Fast, isolated

### 2. Integration Tests (H2 in-memory)
- Schema tạo tự động (ddl-auto=create-drop)
- Data fixtures tạo trong @BeforeEach
- Test full request/response cycle

### 3. Full E2E Tests
- Test cả luồng multi-step (wizard, grading flow)
- Có thể dùng @Sql để load initial data
- Chậm hơn, nhưng cover luồng thực tế

---

## Priority Order (chạy trước)

```
1. WizardValidationServiceTest
   → Fast, pure logic, no DB, no mocks needed
   → 40 tests, cover validation engine

2. NotificationServiceTest
   → Simple service, few deps

3. StudentAssignmentServiceTest
   → Core logic cho Student flow

4. ExpertAssignmentControllerTest
   → First integration layer

5. ExpertApprovalFlowTest
   → Full luồng 2 actors (Expert + Teacher)
```

---

## Các file đã tạo trong docs/

```
docs/superpowers/test-scenarios/
├── README.md                              ← file này
├── SPEC-001-expert-creates-assignment-test-scenarios.md
├── SPEC-002-teacher-creates-lesson-quiz-test-scenarios.md
├── SPEC-003-expert-approves-teacher-questions-test-scenarios.md
├── SPEC-004-student-takes-assignment-test-scenarios.md
├── SPEC-005-teacher-grades-assignment-test-scenarios.md
├── SPEC-006-teacher-grades-lesson-quiz-test-scenarios.md
└── SPEC-question-group-wizard-test-scenarios.md
```
