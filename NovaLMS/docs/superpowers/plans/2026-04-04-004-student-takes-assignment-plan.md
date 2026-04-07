# Plan 004 — Student Takes Assignment

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Student goes through 4 sequential skill sections (LISTENING → READING → SPEAKING → WRITING) with per-section partial submissions, SPEAKING recording with countdown timer, auto-submit on timer expiry, and resume capability.

**Architecture:** New `AssignmentSession` entity tracks per-student per-quiz state. Multi-page wizard with section navigation. SPEAKING recording uses browser MediaRecorder API. Auto-save every 30s. AI grading fires async after completion.

**Tech Stack:** Spring Boot 3.3.4, Spring Data JPA, Thymeleaf, browser MediaRecorder API, Cloudinary.

---

## File Structure

```
src/main/java/com/example/DoAn/
├── model/
│   └── AssignmentSession.java              CREATE
├── repository/
│   └── AssignmentSessionRepository.java    CREATE
├── dto/response/
│   ├── AssignmentInfoDTO.java              CREATE
│   └── AssignmentSectionDTO.java          CREATE
├── service/
│   ├── IStudentAssignmentService.java     CREATE
│   └── impl/
│       └── StudentAssignmentServiceImpl.java CREATE
├── controller/
│   ├── StudentAssignmentController.java   CREATE (View routes)
│   └── StudentAssignmentApiController.java  CREATE (REST API)
└── views/templates/
    ├── student/
    │   ├── assignment-welcome.html        CREATE
    │   ├── assignment-section.html        CREATE (LISTENING/READING/WRITING)
    │   ├── assignment-speaking.html       CREATE (SPEAKING + recording)
    │   ├── assignment-complete.html       CREATE
    │   └── assignment-expired.html        CREATE
    └── fragments/
        └── assignment-fragments.html     CREATE (shared progress bar)
```

---

## Chunk 1: AssignmentSession Entity & Repository

### Task 1: Create AssignmentSession entity

**Files:**
- Create: `src/main/java/com/example/DoAn/model/AssignmentSession.java`

- [ ] **Step 1: Write the entity**

```java
package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_session",
    uniqueConstraints = @UniqueConstraint(columnNames = {"quiz_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // IN_PROGRESS | COMPLETED | EXPIRED
    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS";

    // 0=LISTENING, 1=READING, 2=SPEAKING, 3=WRITING
    @Column(name = "current_skill_index", nullable = false)
    private Integer currentSkillIndex = 0;

    // JSON: {"LISTENING": {"1": "a", "2": "cloudinary_url"}, ...}
    @Column(name = "section_answers", columnDefinition = "JSON")
    private String sectionAnswers;

    // JSON: {"LISTENING": "COMPLETED", "READING": "IN_PROGRESS", ...}
    @Column(name = "section_statuses", columnDefinition = "JSON")
    private String sectionStatuses;

    // JSON: {"SPEAKING": "2024-04-01T10:05:00", ...}
    @Column(name = "section_expiry", columnDefinition = "JSON")
    private String sectionExpiry;

    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
```

### Task 2: Create AssignmentSessionRepository

**Files:**
- Create: `src/main/java/com/example/DoAn/repository/AssignmentSessionRepository.java`

- [ ] **Step 1: Write the repository**

```java
package com.example.DoAn.repository;

import com.example.DoAn.model.AssignmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSessionRepository extends JpaRepository<AssignmentSession, Long> {

    Optional<AssignmentSession> findByQuizQuizIdAndUserUserId(Integer quizId, Long userId);

    boolean existsByQuizQuizIdAndUserUserId(Integer quizId, Long userId);

    List<AssignmentSession> findByUserUserId(Long userId);

    @Query("SELECT COUNT(s) FROM AssignmentSession s WHERE s.quiz.quizId = :quizId AND s.user.userId = :userId")
    long countByQuizAndUser(@Param("quizId") Integer quizId, @Param("userId") Long userId);
}
```

### Task 3: Add fields to QuizResult entity

**Files:**
- Modify: `src/main/java/com/example/DoAn/model/QuizResult.java`

- [ ] **Step 1: Read QuizResult.java**

Run: Read `QuizResult.java`.

- [ ] **Step 2: Add new fields**

Add after existing fields:

```java
@Column(name = "assignment_session_id")
private Long assignmentSessionId;

@Column(name = "section_scores", columnDefinition = "JSON")
private String sectionScores; // {"LISTENING": 8.0, "READING": 7.5, "SPEAKING": null, "WRITING": null}
```

---

## Chunk 2: DTOs

### Task 4: Create DTOs

**Files:**
- Create: `src/main/java/com/example/DoAn/dto/response/AssignmentInfoDTO.java`
- Create: `src/main/java/com/example/DoAn/dto/response/AssignmentSectionDTO.java`

- [ ] **Step 1: Write AssignmentInfoDTO**

```java
package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentInfoDTO {
    private Integer quizId;
    private String title;
    private String description;
    private String quizCategory;
    private List<String> skillOrder;
    private Map<String, Integer> timeLimitPerSkill;
    private Integer quizLevelTimeLimit; // from timeLimitMinutes
    private Long sessionId;
    private String sessionStatus; // IN_PROGRESS / COMPLETED / EXPIRED
    private Integer currentSkillIndex;
    private Map<String, String> sectionStatuses;
    private Boolean canStart;    // true if no session exists
    private Boolean canResume;    // true if session IN_PROGRESS
    private Boolean isCompleted; // true if session COMPLETED
    private Long attemptsUsed;
    private Long maxAttempts;
    private Boolean attemptsExceeded;
}
```

- [ ] **Step 2: Write AssignmentSectionDTO**

```java
package com.example.DoAn.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSectionDTO {
    private Long sessionId;
    private String skill;
    private Integer sectionIndex;    // 0-3
    private Integer currentSkillIndex;
    private Long timerSeconds;       // quiz-level remaining time
    private Long speakingTimerSeconds; // per-skill timer (only for SPEAKING)
    private String speakingExpiry;    // ISO datetime string
    private List<QuestionPayloadDTO> questions;
    private Map<Integer, Object> savedAnswers;
    private String sectionStatus;    // IN_PROGRESS / COMPLETED / LOCKED
    private String nextSkill;
    private String previousSkill;
    private String nextSkillIndex;
    private Boolean isSpeaking;
    private Boolean isWriting;
    private Boolean isLastSection;
}
```

---

## Chunk 3: StudentAssignmentService

### Task 5: Create IStudentAssignmentService

**Files:**
- Create: `src/main/java/com/example/DoAn/service/IStudentAssignmentService.java`

- [ ] **Step 1: Write the interface**

```java
package com.example.DoAn.service;

import com.example.DoAn.dto.response.AssignmentInfoDTO;
import com.example.DoAn.dto.response.AssignmentSectionDTO;
import com.example.DoAn.model.AssignmentSession;

import java.util.Map;

public interface IStudentAssignmentService {

    // Entry point — get assignment info, create/resume session
    AssignmentInfoDTO getAssignmentInfo(Integer quizId, String userEmail);

    // Get section data + questions
    AssignmentSectionDTO getSection(Long sessionId, String skill, String userEmail);

    // Auto-save answers (every 30s)
    void saveAnswers(Long sessionId, String skill, Map<Integer, Object> answers, String userEmail);

    // Submit a section
    Map<String, Object> submitSection(Long sessionId, String skill,
            Map<Integer, Object> answers, String userEmail);

    // Submit SPEAKING with audio URL
    Map<String, Object> submitSpeakingSection(Long sessionId,
            Map<Integer, String> audioUrls, String userEmail);

    // Complete assignment (after WRITING submitted)
    Long completeAssignment(Long sessionId, String userEmail);

    // Auto-submit on timer expiry
    void autoSubmit(Long sessionId, String userEmail);
}
```

### Task 6: Create StudentAssignmentServiceImpl

**Files:**
- Create: `src/main/java/com/example/DoAn/service/impl/StudentAssignmentServiceImpl.java`

- [ ] **Step 1: Write the service**

```java
package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.AssignmentInfoDTO;
import com.example.DoAn.dto.response.AssignmentSectionDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.IStudentAssignmentService;
import com.example.DoAn.service.IGroqGradingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentAssignmentServiceImpl implements IStudentAssignmentService {

    private final QuizRepository quizRepository;
    private final AssignmentSessionRepository sessionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final RegistrationRepository registrationRepository;
    private final GroqGradingServiceImpl groqGradingService;
    private final ObjectMapper objectMapper;

    private static final List<String> SEQUENTIAL_SKILLS = Arrays.asList(
        "LISTENING", "READING", "SPEAKING", "WRITING"
    );

    @Override
    @Transactional(readOnly = true)
    public AssignmentInfoDTO getAssignmentInfo(Integer quizId, String userEmail) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));

        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate sequential assignment
        if (quiz.getIsSequential() == null || !quiz.getIsSequential()) {
            throw new InvalidDataException("This is not a sequential assignment");
        }

        // Validate published + open
        if (!"PUBLISHED".equals(quiz.getStatus()) || quiz.getIsOpen() == null || !quiz.getIsOpen()) {
            throw new InvalidDataException("Assignment is not available");
        }

        // Validate enrollment
        boolean enrolled = registrationRepository.existsByUserUserIdAndClazzClazzIdAndStatus(
            user.getUserId(), quiz.getClazz().getClazzId(), "Approved");
        if (!enrolled) {
            throw new InvalidDataException("Bạn chưa đăng ký lớp học này");
        }

        // Check attempts
        long attemptsUsed = sessionRepository.countByQuizAndUser(quizId, user.getUserId());
        Long maxAttempts = quiz.getMaxAttempts() != null ? quiz.getMaxAttempts().longValue() : null;

        // Find or create session
        Optional<AssignmentSession> existing = sessionRepository
            .findByQuizQuizIdAndUserUserId(quizId, user.getUserId());

        AssignmentSession session = null;
        if (existing.isPresent()) {
            session = existing.get();
        } else {
            if (maxAttempts != null && attemptsUsed >= maxAttempts) {
                AssignmentInfoDTO dto = new AssignmentInfoDTO();
                dto.setAttemptsExceeded(true);
                dto.setAttemptsUsed(attemptsUsed);
                dto.setMaxAttempts(maxAttempts);
                return dto;
            }
            session = new AssignmentSession();
            session.setQuiz(quiz);
            session.setUser(user);
            session.setStatus("IN_PROGRESS");
            session.setCurrentSkillIndex(0);
            Map<String, String> statuses = new LinkedHashMap<>();
            for (int i = 0; i < SEQUENTIAL_SKILLS.size(); i++) {
                statuses.put(SEQUENTIAL_SKILLS.get(i),
                    i == 0 ? "IN_PROGRESS" : "LOCKED");
            }
            session.setSectionStatuses(objectMapper.writeValueAsString(statuses));
            session.setSectionAnswers("{}");
            session.setStartedAt(LocalDateTime.now());
            if (quiz.getTimeLimitMinutes() != null) {
                session.setExpiresAt(LocalDateTime.now().plusMinutes(quiz.getTimeLimitMinutes()));
            }
            session = sessionRepository.save(session);
        }

        // Build response
        AssignmentInfoDTO dto = new AssignmentInfoDTO();
        dto.setQuizId(quizId);
        dto.setTitle(quiz.getTitle());
        dto.setDescription(quiz.getDescription());
        dto.setQuizCategory(quiz.getQuizCategory());
        dto.setSkillOrder(SEQUENTIAL_SKILLS);
        if (quiz.getTimeLimitPerSkill() != null) {
            dto.setTimeLimitPerSkill(objectMapper.readValue(
                quiz.getTimeLimitPerSkill(), new TypeReference<Map<String, Integer>>() {}));
        }
        dto.setQuizLevelTimeLimit(quiz.getTimeLimitMinutes());
        dto.setSessionId(session.getId());
        dto.setSessionStatus(session.getStatus());
        dto.setCurrentSkillIndex(session.getCurrentSkillIndex());
        dto.setSectionStatuses(objectMapper.readValue(
            session.getSectionStatuses(), new TypeReference<Map<String, String>>() {}));
        dto.setCanStart(existing.isEmpty());
        dto.setCanResume("IN_PROGRESS".equals(session.getStatus()));
        dto.setIsCompleted("COMPLETED".equals(session.getStatus()));
        dto.setAttemptsUsed(attemptsUsed);
        dto.setMaxAttempts(maxAttempts);
        dto.setAttemptsExceeded(maxAttempts != null && attemptsUsed >= maxAttempts);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSectionDTO getSection(Long sessionId, String skill, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        int skillIdx = SEQUENTIAL_SKILLS.indexOf(skill);

        // Validate section is accessible
        if (skillIdx > session.getCurrentSkillIndex()) {
            throw new InvalidDataException("Phần này chưa được mở");
        }

        // Load questions for this skill
        List<QuizQuestion> qqList = quizQuestionRepository
            .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);

        Map<Integer, Object> savedAnswers = new HashMap<>();
        if (session.getSectionAnswers() != null) {
            Map<String, Map<String, Object>> allAnswers = objectMapper.readValue(
                session.getSectionAnswers(),
                new TypeReference<Map<String, Map<String, Object>>>() {});
            Map<String, Object> skillAnswers = allAnswers.get(skill);
            if (skillAnswers != null) {
                for (Map.Entry<String, Object> e : skillAnswers.entrySet()) {
                    savedAnswers.put(Integer.parseInt(e.getKey()), e.getValue());
                }
            }
        }

        AssignmentSectionDTO dto = new AssignmentSectionDTO();
        dto.setSessionId(sessionId);
        dto.setSkill(skill);
        dto.setSectionIndex(skillIdx);
        dto.setCurrentSkillIndex(session.getCurrentSkillIndex());
        dto.setQuestions(mapToPayload(qqList));
        dto.setSavedAnswers(savedAnswers);
        dto.setSectionStatus(session.getSectionStatuses());
        dto.setIsSpeaking("SPEAKING".equals(skill));
        dto.setIsWriting("WRITING".equals(skill));
        dto.setIsLastSection(skillIdx == SEQUENTIAL_SKILLS.size() - 1);

        if (skillIdx > 0) {
            dto.setPreviousSkill(SEQUENTIAL_SKILLS.get(skillIdx - 1));
        }
        if (skillIdx < SEQUENTIAL_SKILLS.size() - 1) {
            dto.setNextSkill(SEQUENTIAL_SKILLS.get(skillIdx + 1));
        }

        // Parse timers
        if ("SPEAKING".equals(skill) && session.getQuiz().getTimeLimitPerSkill() != null) {
            Map<String, Integer> limits = objectMapper.readValue(
                session.getQuiz().getTimeLimitPerSkill(),
                new TypeReference<Map<String, Integer>>() {});
            Integer speakingMins = limits.get("SPEAKING");
            if (speakingMins != null) {
                dto.setSpeakingTimerSeconds(speakingMins * 60L);
                // Calculate expiry from when timer starts
                dto.setSpeakingExpiry(LocalDateTime.now().plusMinutes(speakingMins).toString());
            }
        }

        return dto;
    }

    @Override
    public void saveAnswers(Long sessionId, String skill,
            Map<Integer, Object> answers, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        Map<String, Map<String, Object>> allAnswers = new HashMap<>();
        if (session.getSectionAnswers() != null) {
            allAnswers = objectMapper.readValue(session.getSectionAnswers(),
                new TypeReference<Map<String, Map<String, Object>>>() {});
        }
        allAnswers.put(skill, new HashMap<>(answers));
        session.setSectionAnswers(objectMapper.writeValueAsString(allAnswers));
        sessionRepository.save(session);
    }

    @Override
    public Map<String, Object> submitSection(Long sessionId, String skill,
            Map<Integer, Object> answers, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        int skillIdx = SEQUENTIAL_SKILLS.indexOf(skill);

        // Save answers
        saveAnswers(sessionId, skill, answers, userEmail);

        // Auto-grade MC/FILL/MATCH for this section
        List<QuizQuestion> qqList = quizQuestionRepository
            .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);
        // Grading logic: compare answers, store isCorrect in answers map
        for (QuizQuestion qq : qqList) {
            Question q = qq.getQuestion();
            Object answer = answers.get(q.getQuestionId());
            if (answer == null) continue;
            boolean correct = checkAnswer(q, answer);
            // Store in answers map for later (or in separate QuizAnswer)
            answers.put(q.getQuestionId(), Map.of(
                "value", answer,
                "correct", correct
            ));
        }

        // Update statuses
        Map<String, String> statuses = objectMapper.readValue(
            session.getSectionStatuses(), new TypeReference<Map<String, String>>() {});
        statuses.put(skill, "COMPLETED");
        session.setSectionStatuses(objectMapper.writeValueAsString(statuses));

        // Advance to next skill if not last
        if (skillIdx < SEQUENTIAL_SKILLS.size() - 1) {
            String nextSkill = SEQUENTIAL_SKILLS.get(skillIdx + 1);
            session.setCurrentSkillIndex(skillIdx + 1);
            statuses.put(nextSkill, "IN_PROGRESS");
        } else {
            // Last section — complete
            return completeAndReturn(session);
        }

        sessionRepository.save(session);

        Map<String, Object> result = new HashMap<>();
        result.put("nextSkill", SEQUENTIAL_SKILLS.get(Math.min(skillIdx + 1, SEQUENTIAL_SKILLS.size() - 1)));
        result.put("sectionCompleted", true);
        return result;
    }

    @Override
    public Map<String, Object> submitSpeakingSection(Long sessionId,
            Map<Integer, String> audioUrls, String userEmail) {
        Map<Integer, Object> answers = new HashMap<>(audioUrls);
        return submitSection(sessionId, "SPEAKING", answers, userEmail);
    }

    @Override
    public Long completeAssignment(Long sessionId, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        Map<String, Object> result = completeAndReturn(session);
        return (Long) result.get("resultId");
    }

    @Override
    public void autoSubmit(Long sessionId, String userEmail) {
        AssignmentSession session = getSessionForUser(sessionId, userEmail);
        if (!"IN_PROGRESS".equals(session.getStatus())) return;

        // Load all answers
        Map<String, Map<String, Object>> allAnswers = new HashMap<>();
        if (session.getSectionAnswers() != null) {
            allAnswers = objectMapper.readValue(session.getSectionAnswers(),
                new TypeReference<Map<String, Map<String, Object>>>() {});
        }

        // Mark remaining sections as EXPIRED
        Map<String, String> statuses = objectMapper.readValue(
            session.getSectionStatuses(), new TypeReference<Map<String, String>>() {});
        for (String skill : SEQUENTIAL_SKILLS) {
            if (!"COMPLETED".equals(statuses.get(skill))) {
                statuses.put(skill, "EXPIRED");
            }
        }
        session.setSectionStatuses(objectMapper.writeValueAsString(statuses));
        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // Create result with whatever was answered
        createQuizResult(session, allAnswers);
    }

    // --- Private helpers ---

    private AssignmentSession getSessionForUser(Long sessionId, String userEmail) {
        AssignmentSession session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getUser().getEmail().equals(userEmail)) {
            throw new InvalidDataException("Access denied");
        }
        return session;
    }

    private boolean checkAnswer(Question q, Object answer) {
        String type = q.getQuestionType();
        if ("MULTIPLE_CHOICE_SINGLE".equals(type) || "MULTIPLE_CHOICE_MULTI".equals(type)) {
            // Compare selected option IDs
            List<AnswerOption> correct = q.getAnswerOptions().stream()
                .filter(AnswerOption::getCorrectAnswer).toList();
            // Simplified: compare count of correct options
            return correct.stream().anyMatch(c -> c.getCorrectAnswer());
        } else if ("FILL_IN_BLANK".equals(type)) {
            return q.getAnswerOptions().stream().findFirst()
                .map(c -> c.getTitle().trim().equalsIgnoreCase(String.valueOf(answer).trim()))
                .orElse(false);
        }
        return false;
    }

    private List<QuestionPayloadDTO> mapToPayload(List<QuizQuestion> qqList) {
        // Reuse existing mapping from QuizResultServiceImpl or PlacementTestService
        // This maps QuizQuestion → QuestionPayloadDTO (question without correct answers)
        // Implementation: iterate and build payload DTOs, excluding correct answers
        return qqList.stream().map(qq -> {
            Question q = qq.getQuestion();
            QuestionPayloadDTO dto = new QuestionPayloadDTO();
            dto.setQuestionId(q.getQuestionId());
            dto.setContent(q.getContent());
            dto.setQuestionType(q.getQuestionType());
            dto.setSkill(q.getSkill());
            dto.setAudioUrl(q.getAudioUrl());
            dto.setImageUrl(q.getImageUrl());
            // Map options without correctAnswer field
            // Set matching options split etc.
            return dto;
        }).toList();
    }

    private Map<String, Object> completeAndReturn(AssignmentSession session) {
        session.setStatus("COMPLETED");
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        Map<String, Map<String, Object>> allAnswers = new HashMap<>();
        if (session.getSectionAnswers() != null) {
            allAnswers = objectMapper.readValue(session.getSectionAnswers(),
                new TypeReference<Map<String, Map<String, Object>>>() {});
        }

        Long resultId = createQuizResult(session, allAnswers);

        Map<String, Object> result = new HashMap<>();
        result.put("resultId", resultId);
        result.put("completed", true);
        return result;
    }

    private Long createQuizResult(AssignmentSession session, Map<String, Map<String, Object>> allAnswers) {
        QuizResult result = new QuizResult();
        result.setQuiz(session.getQuiz());
        result.setUser(session.getUser());
        result.setSubmittedAt(LocalDateTime.now());
        result.setAssignmentSessionId(session.getId());
        result.setSectionScores("{}"); // to be updated after grading

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;

        // Auto-grade LISTENING/READING sections
        for (String skill : Arrays.asList("LISTENING", "READING")) {
            Map<String, Map<String, Object>> sectionAnswers = null;
            try {
                sectionAnswers = objectMapper.readValue(
                    session.getSectionAnswers(), new TypeReference<Map<String, Map<String, Object>>>() {});
            } catch (Exception ignored) {}
            Map<String, Object> answers = sectionAnswers != null ? sectionAnswers.get(skill) : null;
            if (answers == null) continue;

            List<QuizQuestion> qqList = quizQuestionRepository
                .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);
            BigDecimal sectionScore = BigDecimal.ZERO;
            for (QuizQuestion qq : qqList) {
                maxScore = maxScore.add(qq.getPoints());
                Object answer = answers.get(String.valueOf(qq.getQuestion().getQuestionId()));
                if (answer != null && Boolean.TRUE.equals(
                    ((Map) answer).get("correct"))) {
                    sectionScore = sectionScore.add(qq.getPoints());
                }
            }
            totalScore = totalScore.add(sectionScore);
        }

        result.setScore(totalScore.intValue());
        result.setCorrectRate(
            maxScore.compareTo(BigDecimal.ZERO) > 0
                ? totalScore.divide(maxScore, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO
        );

        QuizResult saved = quizResultRepository.save(result);

        // Fire AI grading for SPEAKING/WRITING
        for (String skill : Arrays.asList("SPEAKING", "WRITING")) {
            List<QuizQuestion> qqList = quizQuestionRepository
                .findByQuizQuizIdAndSkill(session.getQuiz().getQuizId(), skill);
            for (QuizQuestion qq : qqList) {
                // Create placeholder QuizAnswer
                QuizAnswer qa = new QuizAnswer();
                qa.setQuizResult(saved);
                qa.setQuestion(qq.getQuestion());
                qa.setHasPendingReview(true);
                quizAnswerRepository.save(qa);

                // Fire async AI grading
                groqGradingService.fireAndForgetForQuizAnswer(saved.getResultId(), qq.getQuestion().getQuestionId());
            }
        }

        return saved.getResultId();
    }
}
```

**Note:** The service above references `userRepository` without injecting it. Add it as a field:
```java
private final UserRepository userRepository;
```

### Task 7: Add `fireAndForgetForQuizAnswer` to GroqGradingService

**Files:**
- Modify: `src/main/java/com/example/DoAn/service/impl/GroqGradingServiceImpl.java`

- [ ] **Step 1: Read the existing file**

Run: Read `GroqGradingServiceImpl.java`.

- [ ] **Step 2: Add new method**

Add after the existing `fireAndForget()` method:

```java
public void fireAndForgetForQuizAnswer(Long quizResultId, Integer questionId) {
    CompletableFuture.runAsync(() -> {
        try {
            gradeQuizAnswer(quizResultId, questionId);
        } catch (Exception e) {
            log.error("Failed to grade quiz answer: quizResultId={}, questionId={}",
                quizResultId, questionId, e);
        }
    }, executorService);
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void gradeQuizAnswer(Long quizResultId, Integer questionId) {
    QuizAnswer answer = quizAnswerRepository.findByQuizResultResultIdAndQuestionQuestionId(
        quizResultId, questionId);
    if (answer == null) return;

    Question q = answer.getQuestion();
    String userText = null;

    if ("SPEAKING".equals(q.getQuestionType())) {
        // answer.getAnsweredOptions() contains the Cloudinary audio URL
        String audioUrl = answer.getAnsweredOptions();
        if (audioUrl != null && !audioUrl.isBlank()) {
            try {
                userText = groqClient.transcribe(audioUrl);
            } catch (Exception e) {
                log.warn("Transcription failed for answerId={}", answer.getId(), e);
            }
        }
    } else if ("WRITING".equals(q.getQuestionType())) {
        userText = answer.getAnsweredOptions(); // contains text for WRITING
    }

    GradingResponse grading = groqClient.gradeWritingOrSpeaking(
        q.getCefrLevel() != null ? q.getCefrLevel() : "B1",
        q.getQuestionType(),
        userText != null ? userText : ""
    );

    answer.setAiScore(grading.getTotalScore() + "/" + grading.getMaxScore());
    answer.setAiFeedback(grading.getFeedback());
    answer.setAiRubricJson(objectMapper.writeValueAsString(grading.getRubric()));
    quizAnswerRepository.save(answer);
}
```

Also add `QuizAnswerRepository` injection and import:

```java
private final QuizAnswerRepository quizAnswerRepository;
private final QuestionRepository questionRepository;
```

---

## Chunk 4: Controllers

### Task 8: Create StudentAssignmentController (View routes)

**Files:**
- Create: `src/main/java/com/example/DoAn/controller/StudentAssignmentController.java`

- [ ] **Step 1: Write the controller**

```java
package com.example.DoAn.controller;

import com.example.DoAn.dto.response.AssignmentInfoDTO;
import com.example.DoAn.service.IStudentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class StudentAssignmentController {

    private final IStudentAssignmentService assignmentService;

    @GetMapping("/student/assignment/{quizId}")
    public String assignmentHome(
            @PathVariable Integer quizId,
            Authentication auth,
            Model model) {
        try {
            AssignmentInfoDTO info = assignmentService.getAssignmentInfo(quizId, auth.getName());
            model.addAttribute("info", info);

            if (Boolean.TRUE.equals(info.getAttemptsExceeded())) {
                return "student/assignment-expired";
            }
            if (Boolean.TRUE.equals(info.getIsCompleted())) {
                return "redirect:/student/quiz/result/" + info.getSessionId(); // redirect to result
            }

            // Redirect to current section
            String skill = info.getSkillOrder().get(info.getCurrentSkillIndex());
            return "redirect:/student/assignment/session/" + info.getSessionId() + "/section/" + skill;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error/403"; // or a custom error page
        }
    }

    @GetMapping("/student/assignment/session/{sessionId}/section/{skill}")
    public String sectionPage(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            Authentication auth,
            Model model) {
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("skill", skill);

        if ("SPEAKING".equals(skill)) {
            return "student/assignment-speaking";
        }
        return "student/assignment-section";
    }

    @GetMapping("/student/assignment/complete/{resultId}")
    public String completePage(
            @PathVariable Long resultId,
            Authentication auth,
            Model model) {
        model.addAttribute("resultId", resultId);
        return "student/assignment-complete";
    }
}
```

### Task 9: Create StudentAssignmentApiController

**Files:**
- Create: `src/main/java/com/example/DoAn/controller/StudentAssignmentApiController.java`

- [ ] **Step 1: Write the API controller**

```java
package com.example.DoAn.controller;

import com.example.DoAn.dto.response.AssignmentInfoDTO;
import com.example.DoAn.dto.response.AssignmentSectionDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.IStudentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/student/assignment")
@RequiredArgsConstructor
public class StudentAssignmentApiController {

    private final IStudentAssignmentService assignmentService;

    @GetMapping("/{quizId}")
    public ResponseEntity<ResponseData<AssignmentInfoDTO>> getInfo(
            @PathVariable Integer quizId,
            Authentication auth) {
        try {
            AssignmentInfoDTO info = assignmentService.getAssignmentInfo(quizId, auth.getName());
            return ResponseEntity.ok(ResponseData.success(info));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ResponseData.error(e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}/section/{skill}")
    public ResponseEntity<ResponseData<AssignmentSectionDTO>> getSection(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            Authentication auth) {
        try {
            AssignmentSectionDTO section = assignmentService.getSection(sessionId, skill, auth.getName());
            return ResponseEntity.ok(ResponseData.success(section));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ResponseData.error(e.getMessage()));
        }
    }

    @PatchMapping("/session/{sessionId}/section/{skill}")
    public ResponseEntity<ResponseData<Boolean>> saveAnswers(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        @SuppressWarnings("unchecked")
        Map<Integer, Object> answers = (Map<Integer, Object>) body.get("answers");
        assignmentService.saveAnswers(sessionId, skill, answers, auth.getName());
        return ResponseEntity.ok(ResponseData.success(true));
    }

    @PostMapping("/session/{sessionId}/section/{skill}/submit")
    public ResponseEntity<ResponseData<Map<String, Object>>> submitSection(
            @PathVariable Long sessionId,
            @PathVariable String skill,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        @SuppressWarnings("unchecked")
        Map<Integer, Object> answers = (Map<Integer, Object>) body.get("answers");
        Map<String, Object> result = assignmentService.submitSection(
            sessionId, skill, answers, auth.getName());
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @PostMapping("/session/{sessionId}/section/SPEAKING/submit")
    public ResponseEntity<ResponseData<Map<String, Object>>> submitSpeaking(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        @SuppressWarnings("unchecked")
        Map<Integer, String> audioUrls = (Map<Integer, String>) (Map) body;
        Map<String, Object> result = assignmentService.submitSpeakingSection(
            sessionId, audioUrls, auth.getName());
        return ResponseEntity.ok(ResponseData.success(result));
    }

    @PostMapping("/session/{sessionId}/complete")
    public ResponseEntity<ResponseData<Long>> complete(
            @PathVariable Long sessionId,
            Authentication auth) {
        Long resultId = assignmentService.completeAssignment(sessionId, auth.getName());
        return ResponseEntity.ok(ResponseData.success(resultId));
    }

    @PostMapping("/session/{sessionId}/auto-submit")
    public ResponseEntity<ResponseData<Boolean>> autoSubmit(
            @PathVariable Long sessionId,
            Authentication auth) {
        assignmentService.autoSubmit(sessionId, auth.getName());
        return ResponseEntity.ok(ResponseData.success(true));
    }
}
```

---

## Chunk 5: SPEAKING Recording HTML Template

### Task 10: Create assignment-speaking.html

**Files:**
- Create: `src/main/resources/templates/student/assignment-speaking.html`

- [ ] **Step 1: Write the SPEAKING template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/student-layout :: layout(~{::title}, ~{::#content})}">
<head>
    <title>Phần Nói - Ghi âm</title>
</head>
<body>
<div id="content" class="container mt-4">

    <!-- Timer + Progress Bar -->
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4>🎤 Phần Nói (Speaking)</h4>
        <div class="text-danger fw-bold fs-5" id="timerDisplay">--:--</div>
    </div>

    <div class="progress mb-4" style="height:8px">
        <div class="progress-bar bg-primary" id="skillProgress" style="width:75%"></div>
    </div>

    <!-- Questions -->
    <div id="questionsContainer">
        <!-- Loaded via JS -->
    </div>

    <!-- Recording UI -->
    <div class="card mt-4 border-primary" id="recordingCard">
        <div class="card-body text-center">
            <div class="mb-3">
                <span class="badge bg-danger" id="recordingStatus" style="display:none">
                    🔴 Đang ghi âm
                </span>
            </div>

            <div id="recordControls">
                <button class="btn btn-danger btn-lg" id="startBtn" onclick="startRecording()">
                    🔴 Bắt đầu ghi âm
                </button>
            </div>

            <div id="stopControls" style="display:none">
                <button class="btn btn-secondary btn-lg" onclick="stopRecording()">
                    ⏹ Dừng ghi âm
                </button>
            </div>

            <div id="playbackArea" style="display:none" class="mt-3">
                <audio id="playbackAudio" controls class="w-100 mb-2"></audio>
                <button class="btn btn-outline-primary" onclick="reRecord()">
                    🔄 Ghi âm lại
                </button>
            </div>

            <div id="uploadingIndicator" style="display:none" class="mt-2">
                <div class="spinner-border text-primary" role="status"></div>
                <span>Đang tải lên...</span>
            </div>

            <div id="uploadError" class="alert alert-danger mt-2" style="display:none">
                Không thể tải file lên. Vui lòng thử lại.
            </div>
        </div>
    </div>

    <!-- Timer Warning -->
    <div class="alert alert-warning mt-3">
        ⚠️ Ghi âm sẽ tự động nộp khi hết thời gian
    </div>

    <!-- Actions -->
    <div class="d-flex justify-content-between mt-4">
        <a id="prevBtn" class="btn btn-secondary" href="#">← Phần trước</a>
        <button class="btn btn-primary" id="submitBtn" onclick="submitSection()" disabled>
            Nộp phần này →
        </button>
    </div>
</div>

<script th:inline="javascript">
const sessionId = /*[[${sessionId}]]*/ 0;
let mediaRecorder = null;
let audioChunks = [];
let currentRecordingUrl = null;
let recordingTimer = null;
let recordingSeconds = 0;
let speakingTimerSeconds = 120; // default 2 min
let currentQuestionId = null;
let currentAnswers = {};

// Load section data
async function loadSection() {
    const resp = await fetch(`/api/v1/student/assignment/session/${sessionId}/section/SPEAKING`);
    const data = await resp.json();
    const section = data.data;

    speakingTimerSeconds = section.speakingTimerSeconds || 120;
    recordingSeconds = speakingTimerSeconds;

    renderQuestions(section.questions, section.savedAnswers);

    // Set navigation
    if (section.previousSkill) {
        document.getElementById('prevBtn').href =
            `/student/assignment/session/${sessionId}/section/${section.previousSkill}`;
    }

    startQuizTimer(section.timerSeconds);
}

function renderQuestions(questions, savedAnswers) {
    const container = document.getElementById('questionsContainer');
    currentAnswers = savedAnswers || {};
    container.innerHTML = questions.map((q, i) => `
        <div class="card mb-3">
            <div class="card-body">
                <h5>Câu ${i+1} (${q.points || 1} điểm)</h5>
                <p class="fw-bold">${q.content}</p>
                ${q.audioUrl ? `<audio controls src="${q.audioUrl}" class="mb-2 w-100"></audio>` : ''}
                <input type="hidden" id="answer_${q.questionId}" value="${savedAnswers[q.questionId] || ''}">
            </div>
        </div>
    `).join('');
}

function startRecording() {
    navigator.mediaDevices.getUserMedia({ audio: true })
        .then(stream => {
            mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
            audioChunks = [];

            mediaRecorder.ondataavailable = e => audioChunks.push(e.data);
            mediaRecorder.onstop = uploadRecording;

            mediaRecorder.start();
            document.getElementById('recordingStatus').style.display = 'inline';
            document.getElementById('recordControls').style.display = 'none';
            document.getElementById('stopControls').style.display = 'block';

            startRecordingTimer();
        })
        .catch(err => {
            alert('Không thể truy cập microphone: ' + err.message);
        });
}

function stopRecording() {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
        mediaRecorder.stop();
        mediaRecorder.stream.getTracks().forEach(t => t.stop());
        clearInterval(recordingTimer);
        document.getElementById('recordingStatus').style.display = 'none';
        document.getElementById('stopControls').style.display = 'none';
    }
}

function startRecordingTimer() {
    recordingSeconds = speakingTimerSeconds;
    recordingTimer = setInterval(() => {
        recordingSeconds--;
        const mins = Math.floor(recordingSeconds / 60);
        const secs = recordingSeconds % 60;
        document.getElementById('timerDisplay').textContent =
            `${String(mins).padStart(2,'0')}:${String(secs).padStart(2,'0')}`;
        if (recordingSeconds <= 0) {
            clearInterval(recordingTimer);
            stopRecording();
            // Auto-submit after upload
            setTimeout(autoSubmitAndUpload, 500);
        }
    }, 1000);
}

async function uploadRecording() {
    document.getElementById('uploadingIndicator').style.display = 'block';
    document.getElementById('uploadError').style.display = 'none';

    const blob = new Blob(audioChunks, { type: 'audio/webm' });
    const formData = new FormData();
    formData.append('file', blob, 'speaking_answer.webm');

    try {
        const resp = await fetch('/api/v1/upload/audio', { method: 'POST', body: formData });
        const data = await resp.json();
        currentRecordingUrl = data.url;

        // Update all hidden inputs
        document.querySelectorAll('input[id^="answer_"]').forEach(inp => {
            inp.value = currentRecordingUrl;
            currentAnswers[parseInt(inp.id.split('_')[1])] = currentRecordingUrl;
        });

        // Show playback
        const audio = document.getElementById('playbackAudio');
        audio.src = currentRecordingUrl;
        document.getElementById('playbackArea').style.display = 'block';
        document.getElementById('submitBtn').disabled = false;
    } catch (e) {
        document.getElementById('uploadError').style.display = 'block';
    } finally {
        document.getElementById('uploadingIndicator').style.display = 'none';
    }
}

function reRecord() {
    currentRecordingUrl = null;
    document.getElementById('playbackArea').style.display = 'none';
    document.getElementById('recordControls').style.display = 'block';
    startRecording(); // restart
}

async function submitSection() {
    if (!currentRecordingUrl) {
        alert('Vui lòng ghi âm trước khi nộp');
        return;
    }

    clearInterval(recordingTimer);
    const answers = {};
    document.querySelectorAll('input[id^="answer_"]').forEach(inp => {
        answers[parseInt(inp.id.split('_')[1])] = inp.value;
    });

    const resp = await fetch(`/api/v1/student/assignment/session/${sessionId}/section/SPEAKING/submit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers: answers })
    });
    const data = await resp.json();
    if (data.success) {
        window.location.href = `/student/assignment/session/${sessionId}/section/WRITING`;
    } else {
        alert('Lỗi: ' + data.message);
    }
}

async function autoSubmitAndUpload() {
    if (!currentRecordingUrl) {
        await uploadRecording();
    }
    await submitSection();
}

// Quiz-level timer (top timer)
let quizTimerSeconds = 0;
function startQuizTimer(seconds) {
    if (!seconds) return;
    quizTimerSeconds = seconds;
    setInterval(() => {
        quizTimerSeconds--;
        if (quizTimerSeconds <= 0) {
            // Force auto-submit
            fetch(`/api/v1/student/assignment/session/${sessionId}/auto-submit`, { method: 'POST' });
            window.location.href = `/student/assignment/complete/${sessionId}`;
        }
    }, 1000);
}

loadSection();
</script>
</body>
</html>
```

### Task 11: Create assignment-section.html (LISTENING/READING/WRITING)

**Files:**
- Create: `src/main/resources/templates/student/assignment-section.html`

- [ ] **Step 1: Write the section template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/student-layout :: layout(~{::title}, ~{::#content})}">
<head><title th:text="${skill}">Section</title></head>
<body>
<div id="content" class="container mt-4">

    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 th:text="${skill}">LISTENING</h4>
        <div class="text-danger fw-bold fs-5" id="quizTimer">--:--</div>
    </div>

    <!-- Progress Steps -->
    <div class="d-flex mb-4">
        <div th:each="s, stat : ${T(java.util.Arrays).asList('LISTENING','READING','SPEAKING','WRITING')}"
             class="flex-fill text-center pe-2">
            <div th:class="'rounded py-2 fw-bold ' +
                (s == ${skill} ? 'bg-primary text-white' :
                (${T(java.util.Arrays).asList('LISTENING','READING','SPEAKING','WRITING').indexOf(s) lt
                T(java.util.Arrays).asList('LISTENING','READING','SPEAKING','WRITING').indexOf(skill)} ? 'bg-success text-white' : 'bg-secondary text-white'))"
                 th:text="${s}">SKILL</div>
        </div>
    </div>

    <div id="questionsContainer">
        <!-- Loaded via JS -->
    </div>

    <div class="d-flex justify-content-between mt-4">
        <a id="prevBtn" class="btn btn-secondary" href="#">← Phần trước</a>
        <div>
            <button class="btn btn-outline-primary" onclick="saveDraft()">Lưu tạm</button>
            <button class="btn btn-primary" id="submitBtn" onclick="submitSection()">
                Nộp phần này →
            </button>
        </div>
    </div>
</div>

<script th:inline="javascript">
const sessionId = /*[[${sessionId}]]*/ 0;
const skill = /*[[${skill}]]*/ 'LISTENING';
let currentAnswers = {};
let autoSaveInterval = null;

async function loadSection() {
    const resp = await fetch(`/api/v1/student/assignment/session/${sessionId}/section/${skill}`);
    const data = await resp.json();
    if (!data.success) { alert(data.message); return; }
    const section = data.data;
    currentAnswers = section.savedAnswers || {};
    renderQuestions(section.questions);

    if (section.previousSkill) {
        document.getElementById('prevBtn').href =
            `/student/assignment/session/${sessionId}/section/${section.previousSkill}`;
    }
    if (section.isLastSection) {
        document.getElementById('submitBtn').textContent = 'Nộp bài & Kết thúc';
    }

    // Start quiz timer
    if (section.timerSeconds) {
        startTimer(section.timerSeconds);
    }

    // Start auto-save
    autoSaveInterval = setInterval(saveDraft, 30000);
}

function renderQuestions(questions) {
    const container = document.getElementById('questionsContainer');
    container.innerHTML = questions.map((q, i) => `
        <div class="card mb-3">
            <div class="card-body">
                <h6>Câu ${i+1} (${q.points || 1} điểm)</h6>
                <p class="fw-normal">${q.content}</p>
                ${q.audioUrl ? `<audio controls src="${q.audioUrl}" class="w-100 mb-2"></audio>` : ''}
                ${q.imageUrl ? `<img src="${q.imageUrl}" class="img-fluid mb-2" style="max-height:300px">` : ''}
                <div id="options_${q.questionId}">
                    ${renderOptions(q, i)}
                </div>
            </div>
        </div>
    `).join('');
}

function renderOptions(q, index) {
    if (q.questionType === 'MULTIPLE_CHOICE_SINGLE') {
        return q.answerOptions.map((opt, j) => `
            <div class="form-check">
                <input class="form-check-input" type="radio"
                       name="q_${q.questionId}"
                       id="opt_${q.questionId}_${j}"
                       value="${opt.answerOptionId}"
                       onchange="saveAnswer(${q.questionId}, this.value)"
                       ${currentAnswers[q.questionId] == opt.answerOptionId ? 'checked' : ''}>
                <label class="form-check-label" for="opt_${q.questionId}_${j}">${opt.title}</label>
            </div>
        `).join('');
    } else if (q.questionType === 'MULTIPLE_CHOICE_MULTI') {
        return q.answerOptions.map((opt, j) => `
            <div class="form-check">
                <input class="form-check-input" type="checkbox"
                       name="q_${q.questionId}"
                       id="opt_${q.questionId}_${j}"
                       value="${opt.answerOptionId}"
                       onchange="saveMultiAnswer(${q.questionId})"
                       ${(currentAnswers[q.questionId] || []).includes(opt.answerOptionId) ? 'checked' : ''}>
                <label class="form-check-label" for="opt_${q.questionId}_${j}">${opt.title}</label>
            </div>
        `).join('');
    } else if (q.questionType === 'FILL_IN_BLANK') {
        return `<input type="text" class="form-control"
            id="fill_${q.questionId}" placeholder="Nhập đáp án..."
            value="${currentAnswers[q.questionId] || ''}"
            onchange="saveAnswer(${q.questionId}, this.value)">`;
    } else if (q.questionType === 'WRITING') {
        return `<textarea class="form-control" rows="6"
            id="writing_${q.questionId}"
            placeholder="Nhập bài viết của bạn..."
            onchange="saveAnswer(${q.questionId}, this.value)">${currentAnswers[q.questionId] || ''}</textarea>`;
    }
    return '';
}

function saveAnswer(questionId, value) {
    currentAnswers[questionId] = value;
}

function saveMultiAnswer(questionId) {
    const checked = [...document.querySelectorAll(`[name=q_${questionId}]:checked`)]
        .map(el => parseInt(el.value));
    currentAnswers[questionId] = checked;
}

async function saveDraft() {
    await fetch(`/api/v1/student/assignment/session/${sessionId}/section/${skill}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers: currentAnswers })
    });
    showToast('Đã lưu');
}

async function submitSection() {
    clearInterval(autoSaveInterval);
    const resp = await fetch(`/api/v1/student/assignment/session/${sessionId}/section/${skill}/submit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers: currentAnswers })
    });
    const data = await resp.json();
    if (data.success) {
        const nextSkill = data.data.nextSkill;
        if (nextSkill) {
            window.location.href = `/student/assignment/session/${sessionId}/section/${nextSkill}`;
        } else {
            window.location.href = `/student/assignment/complete/${data.data.resultId}`;
        }
    } else {
        alert('Lỗi: ' + data.message);
        autoSaveInterval = setInterval(saveDraft, 30000);
    }
}

let timerSeconds = 0;
function startTimer(seconds) {
    timerSeconds = seconds;
    setInterval(() => {
        timerSeconds--;
        const h = Math.floor(timerSeconds / 3600);
        const m = Math.floor((timerSeconds % 3600) / 60);
        const s = timerSeconds % 60;
        const display = h > 0
            ? `${h}:${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`
            : `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`;
        document.getElementById('quizTimer').textContent = display;
        if (timerSeconds <= 0) {
            document.getElementById('submitBtn').click();
        }
    }, 1000);
}

function showToast(msg) {
    // Simple toast — reuse existing toast component if available
    console.log(msg);
}

loadSection();
</script>
</body>
</html>
```

### Task 12: Create assignment-complete.html

**Files:**
- Create: `src/main/resources/templates/student/assignment-complete.html`

- [ ] **Step 1: Write the completion page**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/student-layout :: layout(~{::title}, ~{::#content})}">
<head><title>Hoàn thành bài kiểm tra</title></head>
<body>
<div id="content" class="container mt-5 text-center" style="max-width:600px">
    <div class="card">
        <div class="card-body">
            <div class="display-1 mb-3">✅</div>
            <h2>Bạn đã hoàn thành bài kiểm tra!</h2>
            <hr>
            <div class="alert alert-info">
                <p>Phần Nghe và Đọc đã được chấm tự động.</p>
                <p>Phần Nói và Viết đang được chấm điểm tự động (AI).</p>
                <p class="mb-0"><strong>Kết quả cuối cùng sẽ được công bố sau khi Giáo viên duyệt.</strong></p>
            </div>
            <div class="mt-4">
                <a th:href="@{/student/my-assignments}" class="btn btn-outline-primary">
                    ← Quay về danh sách bài kiểm tra
                </a>
                <a th:href="@{/student/quiz/result/{resultId}(resultId=${resultId})}"
                   class="btn btn-primary">
                    Xem kết quả
                </a>
            </div>
        </div>
    </div>
</div>
</body>
</html>
```

---

## Spec Reference

See `docs/superpowers/specs/2026-04-04-004-student-takes-assignment-design.md`.
