# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NovaLMS is a full-stack Learning Management System for English language training (IELTS, TOEIC, Communication). It uses Spring Boot 3.3.4 with Java 17, Thymeleaf server-rendered views + REST API, and MySQL 8.0.

## Build & Run

```bash
# Build
cd NovaLMS/DoAn
./mvnw clean install

# Run (dev)
./mvnw spring-boot:run

# Run JAR
java -jar target/DoAn-0.0.1-SNAPSHOT.jar
```

The app runs at `http://localhost:8080`. No system Maven or Java installation needed — the project includes `mvnw` / `mvnw.cmd`.

**Prerequisites:** MySQL 8.0 running on `localhost:3306`, database `nova_1` created.

## Architecture

### Dual Controller Pattern
The project mixes two controller styles in the same package:

- **View Controllers** (`@Controller`) — return Thymeleaf view names as `String`. Route to `/student/**`, `/teacher/**`, `/expert/**`, etc.
- **API Controllers** (`@RestController`) — return `ResponseData<T>` wrapped JSON. Route to `/api/v1/**`

Always check both controller types when modifying a feature. A given entity (e.g., Quiz) often has both a View Controller and an API Controller.

### Service Pattern
- Interfaces (`I*Service`) + implementations (`*ServiceImpl`) in `service/impl/`
- `@RequiredArgsConstructor` for DI (Lombok)
- `@Transactional` on writes, `@Transactional(readOnly = true)` on reads
- Write service interfaces first before implementations

### Async / Background Tasks
`@EnableAsync` is on the main application class. AI grading uses `CompletableFuture.runAsync()` to fire after transaction commits — never call external services synchronously inside a transaction that hasn't committed yet.

## Data Model

### Quiz = Assessment Unit
`Quiz` is the central entity. Its `quizCategory` field determines behavior:

| Category | Meaning | Creator |
|---|---|---|
| `ENTRY_TEST` | Guest placement test | Expert |
| `COURSE_QUIZ` | Course-level quiz | Expert |
| `MODULE_QUIZ` | Module-level quiz | Expert |
| `LESSON_QUIZ` | Lesson practice quiz | Teacher |
| `COURSE_ASSIGNMENT` | 4-skill sequential assignment | Expert |
| `MODULE_ASSIGNMENT` | 4-skill sequential assignment | Expert |

`Quiz.status`: `DRAFT` → `PUBLISHED` → `ARCHIVED`. Teacher separately toggles `isOpen` to control student access independently of publish status.

### Question Bank
Questions are reusable across quizzes via the `quiz_question` join table. A question's `skill` field is `LISTENING | READING | WRITING | SPEAKING`. Question types: `MULTIPLE_CHOICE_SINGLE | MULTIPLE_CHOICE_MULTI | FILL_IN_BLANK | MATCHING | WRITING | SPEAKING`.

Questions have two sources: `EXPERT_BANK` (shared) and `TEACHER_PRIVATE` (teacher-only). `TEACHER_PRIVATE` questions auto-set to `PENDING_REVIEW` and require Expert approval before entering the shared bank.

### Grading Pipeline
- **MC/FILL/MATCH**: auto-graded synchronously on submit
- **WRITING/SPEAKING**: `isCorrect = null`, marked pending manual review
- AI grading fires async via `GroqGradingService.fireAndForget()` using `TransactionSynchronizationManager.afterCommit()`

### Role System
Roles are stored in the `setting` table (`setting_type = 'ROLE'`), not Spring Security authorities. User → `setting` via `role_id` FK. Role codes: `ADMIN=201`, `MANAGER=202`, `EXPERT=203`, `TEACHER=204`, `STUDENT=205`.

## External Integrations

| Service | Purpose | Config |
|---|---|---|
| **Groq API** | AI question generation + grading (LLaMA 3.3 + Whisper) | `groq.api.url`, `groq.api.key`, `groq.model` |
| **Cloudinary** | Audio/image/video upload | `cloudinary.cloud-name`, `api-key`, `api-secret` |
| **PayOS** | Course payment gateway | `ayos.*` config |
| **Gmail SMTP** | Email (verification, password reset) | `spring.mail.*` |

GroqClient (`service/GroqClient.java`) uses Spring `WebClient`. AIQuestionService uses OkHttp directly (separate HTTP client from RestTemplate).

## Key File Locations

- Entry point: `DoAnApplication.java`
- Security: `configuration/SecurityConfig.java` (session + JWT + OAuth2)
- JWT filter: `configuration/JwtAuthenticationFilter.java`
- Global error handling: `exception/GlobalExceptionHandler.java`
- REST response wrapper: `dto/response/ResponseData.java`
- Question creation (AI): `service/AIQuestionService.java`
- AI grading: `service/impl/GroqGradingServiceImpl.java`
- Grading pipeline: `service/impl/QuizResultServiceImpl.java`
- Expert quiz CRUD: `service/impl/ExpertQuizServiceImpl.java`
- HTML templates: `src/main/resources/templates/{admin,teacher,expert,student,public,auth}/`

## Critical Red Flags

- **Credentials in `application.properties`** — MySQL password, JWT secret, OAuth client secrets, Cloudinary keys, PayOS keys, SMTP password are all hardcoded. Never commit real credentials.
- **`groq.api.key` may be missing** from `application.properties` — the `GroqClient` uses `@Value("${groq.api.key}")` but the property may not be in the file. The app will fail to start if Groq is used without it.
- **`ddl-auto=update`** in production config — Hibernate auto-mutates the schema on startup.
- **`spring-boot-starter-webflux` alongside `spring-boot-starter-web`** — reactive and servlet stacks conflict; only one should be used.
- **No test coverage** — only a context-loads smoke test exists.

## Session Memory

Important session decisions and context are stored in `.claude/session.md` and `.claude/plans/`. Read these before starting new work in a session.

## Spec Documents

Implementation specs are in `docs/superpowers/specs/`. Read the relevant spec before implementing any feature. Current specs cover: hybrid placement tests, AI grading, module/lesson/quiz design, and 4-skill sequential assignments (SPEC 001–006).
