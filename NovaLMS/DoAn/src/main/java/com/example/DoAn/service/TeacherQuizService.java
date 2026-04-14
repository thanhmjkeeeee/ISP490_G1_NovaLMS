package com.example.DoAn.service;

import com.example.DoAn.dto.request.QuestionBankRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.QuizSkillSummaryDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherQuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserRepository userRepository;
    private final ClazzRepository clazzRepository;
    private final QuizResultRepository quizResultRepository;
    private final ClassSessionRepository classSessionRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;
    private final INotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> NO_OPTIONS_TYPES = Set.of("WRITING", "SPEAKING");
    private static final Set<String> VALID_QUESTION_TYPES = Set.of(
            "MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK",
            "MATCHING", "WRITING", "SPEAKING"
    );
    private static final Set<String> VALID_SKILLS = Set.of("LISTENING", "READING", "WRITING", "SPEAKING");
    private static final Set<String> VALID_CEFR = Set.of("A1", "A2", "B1", "B2", "C1", "C2");
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");

    // ═══════════════════════════════════════════════════════════════════════
    //  QUIZ MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Tạo quiz mới cho teacher.
     * Supports COURSE_QUIZ (requires classId) and LESSON_QUIZ (requires lessonId).
     */
    @Transactional
    public ResponseData<TeacherQuizDTO> createQuiz(QuizRequestDTO request, String email) {
        try {
            User teacher = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            if (request.getTitle() == null || request.getTitle().isBlank()) {
                return ResponseData.error(400, "Tên quiz không được để trống");
            }

            Quiz quiz = Quiz.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .quizCategory(request.getQuizCategory() != null ? request.getQuizCategory() : "COURSE_QUIZ")
                    .status("DRAFT")
                    .timeLimitMinutes(request.getTimeLimitMinutes())
                    .passScore(request.getPassScore())
                    .maxAttempts(request.getMaxAttempts())
                    .numberOfQuestions(request.getNumberOfQuestions())
                    .questionOrder(request.getQuestionOrder() != null ? request.getQuestionOrder() : "FIXED")
                    .showAnswerAfterSubmit(request.getShowAnswerAfterSubmit() != null ? request.getShowAnswerAfterSubmit() : false)
                    .user(teacher)
                    .build();

            // LESSON_QUIZ — attach to lesson
            if ("LESSON_QUIZ".equals(request.getQuizCategory())) {
                if (request.getLessonId() == null) {
                    return ResponseData.error(400, "Phải gắn quiz với một bài học (lessonId)");
                }
                Lesson lesson = lessonRepository.findById(request.getLessonId()).orElse(null);
                if (lesson == null) {
                    return ResponseData.error(404, "Không tìm thấy bài học");
                }
                quiz.setLesson(lesson);
                // Auto-derive course from lesson's module
                if (lesson.getModule() != null && lesson.getModule().getCourse() != null) {
                    quiz.setCourse(lesson.getModule().getCourse());
                }
            } else {
                // COURSE_QUIZ or default — requires classId
                if (request.getClassId() == null) {
                    return ResponseData.error(400, "Phải gắn quiz với một lớp học (classId)");
                }
                Clazz clazz = clazzRepository.findById(request.getClassId()).orElse(null);
                if (clazz == null) {
                    return ResponseData.error(404, "Không tìm thấy lớp học");
                }
                quiz.setClazz(clazz);
                quiz.setCourse(clazz.getCourse());
            }

            // Override with explicit courseId if provided
            if (request.getCourseId() != null) {
                Course course = courseRepository.findById(request.getCourseId()).orElse(null);
                if (course != null) quiz.setCourse(course);
            }

            quizRepository.save(quiz);
            return ResponseData.success("Tạo quiz thành công", toTeacherQuizDTO(quiz));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Cập nhật quiz.
     */
    @Transactional
    public ResponseData<TeacherQuizDTO> updateQuiz(Integer quizId, QuizRequestDTO request, String email) {
        try {
            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");

            boolean hasAttempts = quizResultRepository.countByQuizQuizId(quizId) > 0;
            if (hasAttempts && !"DRAFT".equals(quiz.getStatus())) {
                return ResponseData.error(400, "Không thể cập nhật quiz đã có học viên làm bài");
            }

            if (request.getTitle() != null) quiz.setTitle(request.getTitle());
            if (request.getDescription() != null) quiz.setDescription(request.getDescription());
            if (request.getTimeLimitMinutes() != null) quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
            if (request.getPassScore() != null) quiz.setPassScore(request.getPassScore());
            if (request.getMaxAttempts() != null) quiz.setMaxAttempts(request.getMaxAttempts());
            if (request.getNumberOfQuestions() != null) quiz.setNumberOfQuestions(request.getNumberOfQuestions());
            if (request.getQuestionOrder() != null) quiz.setQuestionOrder(request.getQuestionOrder());
            if (request.getShowAnswerAfterSubmit() != null) quiz.setShowAnswerAfterSubmit(request.getShowAnswerAfterSubmit());
            if (request.getStatus() != null) quiz.setStatus(request.getStatus());

            quizRepository.save(quiz);
            return ResponseData.success("Cập nhật thành công", toTeacherQuizDTO(quiz));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Publish quiz (chuyển từ DRAFT -> PUBLISHED).
     */
    @Transactional
    public ResponseData<TeacherQuizDTO> publishQuiz(Integer quizId, String email) {
        try {
            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");

            if ("PUBLISHED".equals(quiz.getStatus())) {
                return ResponseData.error(400, "Quiz đã được publish rồi");
            }
            if ("ARCHIVED".equals(quiz.getStatus())) {
                return ResponseData.error(400, "Quiz đã bị archived, không thể publish");
            }

            // Kiểm tra quiz có ít nhất 1 câu hỏi
            int qCount = quizQuestionRepository.countByQuizQuizId(quizId);
            if (qCount == 0) {
                return ResponseData.error(400, "Quiz phải có ít nhất 1 câu hỏi trước khi publish");
            }

            quiz.setStatus("PUBLISHED");
            quizRepository.save(quiz);

            // ── Notify enrolled students ────────────────────────────────────────
            notifyStudentsQuizPublished(quiz, null, "PUBLISHED");

            return ResponseData.success("Quiz đã được publish! Sinh viên có thể làm bài.", toTeacherQuizDTO(quiz));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Mở/đóng quiz cho học sinh làm (toggle isOpen).
     * Teacher có thể mở/đóng quiz bất kỳ lúc nào mà không cần đổi status DRAFT/PUBLISHED.
     */
    @Transactional
    public ResponseData<TeacherQuizDTO> toggleQuizOpen(Integer quizId, String email) {
        try {
            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");

            // Toggle: false -> true (mở), true -> false (đóng)
            Boolean current = quiz.getIsOpen();
            Boolean updated = (current == null || !current) ? true : false;
            quiz.setIsOpen(updated);
            quizRepository.save(quiz);

            String action = updated ? "mở" : "đóng";
            return ResponseData.success("Quiz đã được " + action + "! Học sinh có thể làm bài.",
                    toTeacherQuizDTO(quiz));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Xóa quiz.
     */
    @Transactional
    public ResponseData<Void> deleteQuiz(Integer quizId, String email) {
        try {
            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");

            if (quizResultRepository.countByQuizQuizId(quizId) > 0) {
                return ResponseData.error(400, "Không thể xóa quiz đã có học viên làm bài");
            }

            quizRepository.delete(quiz);
            return ResponseData.success("Xóa quiz thành công");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Lấy chi tiết quiz cho teacher.
     */
    @Transactional(readOnly = true)
    public ResponseData<TeacherQuizDTO> getQuizById(Integer quizId) {
        try {
            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");
            return ResponseData.success("Chi tiết quiz", toTeacherQuizDTO(quiz));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Lấy tất cả COURSE_QUIZ của course mà classId thuộc về.
     * API: GET /api/v1/teacher/quizzes/class/{classId}
     * - Xác thực: classId phải thuộc một lớp mà teacher phụ trách
     * - Trả về: TẤT CẢ quiz (DRAFT + PUBLISHED) của course của lớp đó
     */
    @Transactional(readOnly = true)
    public ResponseData<List<TeacherQuizDTO>> getQuizzesByClass(Integer classId, String email) {
        try {
            // 1. Xác thực: tìm teacher theo email
            User teacher = userRepository.findByEmail(email).orElse(null);
            if (teacher == null) {
                return ResponseData.error(401, "Không tìm thấy người dùng");
            }

            // 2. Lấy tất cả lớp mà teacher phụ trách
            List<Clazz> teacherClasses = clazzRepository.findAllByTeacher_UserId(teacher.getUserId());
            if (teacherClasses.isEmpty()) {
                return ResponseData.success("Không có lớp nào được phân công", List.of());
            }

            // 3. Kiểm tra classId có thuộc lớp của teacher không
            Clazz targetClass = teacherClasses.stream()
                    .filter(c -> c.getClassId().equals(classId))
                    .findFirst().orElse(null);
            if (targetClass == null) {
                return ResponseData.error(403, "Bạn không phải giáo viên của lớp này");
            }

            // 4. Lấy quiz của course mà lớp này thuộc về
            List<Quiz> quizzes = List.of();
            if (targetClass.getCourse() != null) {
                quizzes = quizRepository.findAllByCourseCourseIdIn(
                        List.of(targetClass.getCourse().getCourseId()));
            }

            List<TeacherQuizDTO> dtos = quizzes.stream()
                    .map(this::toTeacherQuizDTO)
                    .collect(Collectors.toList());
            return ResponseData.success("Danh sách quiz", dtos);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  QUESTION MANAGEMENT (TEACHER PRIVATE)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Tạo câu hỏi PRIVATE cho quiz.
     * - source = TEACHER_PRIVATE
     * - status = PENDING_REVIEW (chờ expert duyệt mới vào bank)
     */
    @Transactional
    public ResponseData<TeacherQuestionDTO> createQuestion(QuestionBankRequestDTO request, String email) {
        try {
            User teacher = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Validate metadata
            if (!VALID_QUESTION_TYPES.contains(request.getQuestionType())) {
                return ResponseData.error(400, "Loại câu hỏi không hợp lệ: " + request.getQuestionType());
            }
            if (!VALID_SKILLS.contains(request.getSkill())) {
                return ResponseData.error(400, "Kỹ năng không hợp lệ: " + request.getSkill());
            }
            if (!VALID_CEFR.contains(request.getCefrLevel())) {
                return ResponseData.error(400, "Cấp độ CEFR không hợp lệ: " + request.getCefrLevel());
            }

            validateAnswerOptions(request);

            Question question = Question.builder()
                    .content(request.getContent())
                    .questionType(request.getQuestionType())
                    .skill(request.getSkill())
                    .cefrLevel(request.getCefrLevel())
                    .topic(request.getTopic())
                    .tags(request.getTags())
                    .explanation(request.getExplanation())
                    .audioUrl(request.getAudioUrl())
                    .imageUrl(request.getImageUrl())
                    .status("DRAFT")
                    .source("TEACHER_PRIVATE")
                    .user(teacher)
                    .build();

            questionRepository.save(question);

            // Save answer options
            if (request.getOptions() != null && !request.getOptions().isEmpty()) {
                for (int i = 0; i < request.getOptions().size(); i++) {
                    QuestionBankRequestDTO.AnswerOptionDTO optDTO = request.getOptions().get(i);
                    AnswerOption opt = AnswerOption.builder()
                            .question(question)
                            .title(optDTO.getTitle())
                            .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                            .orderIndex(optDTO.getOrderIndex() != null ? optDTO.getOrderIndex() : i)
                            .matchTarget(optDTO.getMatchTarget())
                            .build();
                    answerOptionRepository.save(opt);
                }
            }

            return ResponseData.success("Tạo câu hỏi thành công (chờ duyệt để thêm vào bank)", toTeacherQuestionDTO(question));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Thêm câu hỏi (từ bank đã PUBLISHED hoặc từ private đã tạo) vào quiz.
     * @param quizId quiz cần thêm câu hỏi
     * @param questionId câu hỏi cần thêm (từ bank đã publish hoặc private đã tạo)
     * @param request metadata (orderIndex, points) - nullable
     */
    @Transactional
    public ResponseData<TeacherQuizDTO> addQuestionToQuiz(Integer quizId, Integer questionId,
            Integer orderIndex, BigDecimal points, String email) {
        try {
            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");

            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null) return ResponseData.error(404, "Không tìm thấy câu hỏi");

            // Chỉ cho phép thêm: PUBLISHED (từ bank) hoặc PENDING_REVIEW (private của chính teacher)
            String allowed = question.getStatus();
            boolean canAdd = "PUBLISHED".equals(allowed) || "PENDING_REVIEW".equals(allowed);
            if (!canAdd) {
                return ResponseData.error(400, "Câu hỏi phải ở trạng thái Published hoặc đang chờ duyệt");
            }

            // Kiểm tra duplicate
            if (quizQuestionRepository.existsByQuizQuizIdAndQuestionQuestionId(quizId, questionId)) {
                return ResponseData.error(400, "Câu hỏi này đã có trong quiz");
            }

            int currentCount = quizQuestionRepository.countByQuizQuizId(quizId);
            QuizQuestion qq = QuizQuestion.builder()
                    .quiz(quiz)
                    .question(question)
                    .orderIndex(orderIndex != null ? orderIndex : currentCount + 1)
                    .points(points != null ? points : BigDecimal.ONE)
                    .build();

            quizQuestionRepository.save(qq);
            return ResponseData.success("Thêm câu hỏi vào quiz thành công", toTeacherQuizDTO(quiz));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Xóa câu hỏi khỏi quiz.
     */
    @Transactional
    public ResponseData<TeacherQuizDTO> removeQuestionFromQuiz(Integer quizId, Integer questionId, String email) {
        try {
            Quiz quiz = quizRepository.findById(quizId).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");

            if (!quizQuestionRepository.existsByQuizQuizIdAndQuestionQuestionId(quizId, questionId)) {
                return ResponseData.error(404, "Câu hỏi không tồn tại trong quiz này");
            }

            quizQuestionRepository.deleteByQuizQuizIdAndQuestionQuestionId(quizId, questionId);
            return ResponseData.success("Xóa câu hỏi khỏi quiz thành công", toTeacherQuizDTO(quiz));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Gửi câu hỏi lên expert để duyệt (chuyển từ PENDING_REVIEW -> submitted for review).
     * Thực chất đã ở PENDING_REVIEW khi tạo, action này có thể dùng để resubmit.
     */
    @Transactional
    public ResponseData<TeacherQuestionDTO> submitQuestionForReview(Integer questionId, String email) {
        try {
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null) return ResponseData.error(404, "Không tìm thấy câu hỏi");

            // Verify ownership
            if (question.getUser() == null || !question.getUser().getEmail().equals(email)) {
                return ResponseData.error(403, "Bạn không sở hữu câu hỏi này");
            }

            if ("PUBLISHED".equals(question.getStatus())) {
                return ResponseData.error(400, "Câu hỏi đã được duyệt rồi");
            }

            question.setStatus("PENDING_REVIEW");
            questionRepository.save(question);
            return ResponseData.success("Đã gửi câu hỏi chờ duyệt", toTeacherQuestionDTO(question));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Gửi nhiều câu hỏi lên expert để duyệt.
     */
    @Transactional
    public ResponseData<Void> submitQuestionsBatch(List<Integer> questionIds, String email) {
        try {
            User teacher = userRepository.findByEmail(email).orElse(null);
            if (teacher == null) return ResponseData.error(401, "Unauthorized");

            List<Question> questions = questionRepository.findAllById(questionIds);
            for (Question q : questions) {
                if (q.getUser() != null && q.getUser().getUserId().equals(teacher.getUserId())) {
                    if ("DRAFT".equals(q.getStatus())) {
                        q.setStatus("PENDING_REVIEW");
                    }
                }
            }
            questionRepository.saveAll(questions);
            return ResponseData.success("Đã gửi " + questions.size() + " câu hỏi chờ duyệt");
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Lấy danh sách câu hỏi từ bank (chỉ PUBLISHED).
     */
    @Transactional(readOnly = true)
    public ResponseData<List<QuestionBankSimpleDTO>> getPublishedQuestions(
            String skill, String cefrLevel, String questionType, String keyword) {
        try {
            List<Question> questions = questionRepository.findAll().stream()
                    .filter(q -> "PUBLISHED".equals(q.getStatus()))
                    .filter(q -> skill == null || skill.isBlank() || skill.equalsIgnoreCase(q.getSkill()))
                    .filter(q -> cefrLevel == null || cefrLevel.isBlank() || cefrLevel.equalsIgnoreCase(q.getCefrLevel()))
                    .filter(q -> questionType == null || questionType.isBlank() || questionType.equalsIgnoreCase(q.getQuestionType()))
                    .filter(q -> keyword == null || keyword.isBlank() ||
                            (q.getContent() != null && q.getContent().toLowerCase().contains(keyword.toLowerCase())))
                    .collect(Collectors.toList());

            List<QuestionBankSimpleDTO> dtos = questions.stream().map(q -> {
                List<AnswerOption> opts = answerOptionRepository.findByQuestionQuestionId(q.getQuestionId());
                List<QuestionBankSimpleDTO.AnswerOptionSimpleDTO> optDTOs = opts.stream()
                        .map(o -> QuestionBankSimpleDTO.AnswerOptionSimpleDTO.builder()
                                .answerOptionId(o.getAnswerOptionId())
                                .title(o.getTitle())
                                .correctAnswer(o.getCorrectAnswer())
                                .orderIndex(o.getOrderIndex())
                                .matchTarget(o.getMatchTarget())
                                .build())
                        .collect(Collectors.toList());

                Integer groupId = q.getQuestionGroup() != null ? q.getQuestionGroup().getGroupId() : null;
                String groupContent = q.getQuestionGroup() != null ? q.getQuestionGroup().getGroupContent() : null;

                return QuestionBankSimpleDTO.builder()
                        .questionId(q.getQuestionId())
                        .content(q.getContent())
                        .questionType(q.getQuestionType())
                        .skill(q.getSkill())
                        .cefrLevel(q.getCefrLevel())
                        .topic(q.getTopic())
                        .status(q.getStatus())
                        .groupId(groupId)
                        .groupContent(groupContent)
                        .options(optDTOs)
                        .build();
            }).collect(Collectors.toList());

            return ResponseData.success("Danh sách câu hỏi", dtos);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Lấy skill summary cho quiz: số câu hỏi đã publish / đang chờ duyệt theo từng kỹ năng.
     */
    @Transactional(readOnly = true)
    public List<QuizSkillSummaryDTO> getSkillSummary(Integer quizId) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizQuizId(quizId);
        Map<String, QuizSkillSummaryDTO> map = new LinkedHashMap<>();

        for (QuizQuestion qq : questions) {
            Question q = qq.getQuestion();
            String skill = q.getSkill() != null ? q.getSkill() : "OTHER";
            map.computeIfAbsent(skill, s -> new QuizSkillSummaryDTO(s, 0, 0, 0, 0));

            QuizSkillSummaryDTO dto = map.get(skill);
            dto.setTotalCount(dto.getTotalCount() + 1);
            if ("PUBLISHED".equals(q.getStatus())) {
                dto.setPublishedCount(dto.getPublishedCount() + 1);
            } else if ("PENDING_REVIEW".equals(q.getStatus())) {
                dto.setPendingCount(dto.getPendingCount() + 1);
            }
            if (qq.getPoints() != null) {
                dto.setTotalPoints(dto.getTotalPoints() + qq.getPoints().intValue());
            }
        }

        return new ArrayList<>(map.values());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void validateAnswerOptions(QuestionBankRequestDTO request) {
        String type = request.getQuestionType();
        if (NO_OPTIONS_TYPES.contains(type)) return;

        if (request.getOptions() == null || request.getOptions().isEmpty()) {
            throw new RuntimeException("Câu hỏi loại " + type + " phải có ít nhất 2 đáp án");
        }

        if ("MULTIPLE_CHOICE_SINGLE".equals(type) || "MULTIPLE_CHOICE_MULTI".equals(type)) {
            if (request.getOptions().size() < 2) {
                throw new RuntimeException("Câu hỏi trắc nghiệm phải có ít nhất 2 đáp án");
            }
            long correctCount = request.getOptions().stream()
                    .filter(o -> Boolean.TRUE.equals(o.getCorrect())).count();
            if (correctCount == 0) {
                throw new RuntimeException("Phải có ít nhất 1 đáp án đúng");
            }
            if ("MULTIPLE_CHOICE_SINGLE".equals(type) && correctCount > 1) {
                throw new RuntimeException("Câu hỏi Single Choice chỉ được có 1 đáp án đúng");
            }
        }

        if ("MATCHING".equals(type)) {
            boolean allHaveTarget = request.getOptions().stream()
                    .allMatch(o -> o.getMatchTarget() != null && !o.getMatchTarget().isBlank());
            if (!allHaveTarget) {
                throw new RuntimeException("Câu hỏi Matching: mỗi đáp án phải có match_target");
            }
        }

        if ("FILL_IN_BLANK".equals(type)) {
            long correctCount = request.getOptions().stream()
                    .filter(o -> Boolean.TRUE.equals(o.getCorrect())).count();
            if (correctCount == 0) {
                throw new RuntimeException("Câu hỏi Fill in the Blank phải có ít nhất 1 đáp án đúng");
            }
        }
    }

    private TeacherQuizDTO toTeacherQuizDTO(Quiz quiz) {
        List<QuizQuestion> quizQuestions = quizQuestionRepository.findByQuizQuizIdOrderByOrderIndexAsc(quiz.getQuizId());
        boolean hasAttempts = quizResultRepository.countByQuizQuizId(quiz.getQuizId()) > 0;

        List<TeacherQuizDTO.QuizQuestionSimpleDTO> questionDTOs = quizQuestions.stream()
                .map(qq -> {
                    Question q = qq.getQuestion();
                    List<AnswerOption> opts = answerOptionRepository.findByQuestionQuestionId(q.getQuestionId());
                    List<TeacherQuizDTO.AnswerOptionSimpleDTO> optDTOs = opts.stream()
                            .map(o -> TeacherQuizDTO.AnswerOptionSimpleDTO.builder()
                                    .answerOptionId(o.getAnswerOptionId())
                                    .title(o.getTitle())
                                    .correctAnswer(o.getCorrectAnswer())
                                    .orderIndex(o.getOrderIndex())
                                    .build())
                            .collect(Collectors.toList());

                    return TeacherQuizDTO.QuizQuestionSimpleDTO.builder()
                            .quizQuestionId(qq.getQuizQuestionId())
                            .questionId(q.getQuestionId())
                            .content(q.getContent())
                            .questionType(q.getQuestionType())
                            .skill(q.getSkill())
                            .cefrLevel(q.getCefrLevel())
                            .status(q.getStatus())
                            .source(q.getSource())
                            .orderIndex(qq.getOrderIndex())
                            .points(qq.getPoints())
                            .options(optDTOs)
                            .build();
                })
                .collect(Collectors.toList());

        return TeacherQuizDTO.builder()
                .quizId(quiz.getQuizId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .quizCategory(quiz.getQuizCategory())
                .classId(quiz.getClazz() != null ? quiz.getClazz().getClassId() : null)
                .className(quiz.getClazz() != null ? quiz.getClazz().getClassName() : null)
                .courseId(quiz.getCourse() != null ? quiz.getCourse().getCourseId() : null)
                .courseName(quiz.getCourse() != null ? quiz.getCourse().getCourseName() : null)
                .status(quiz.getStatus())
                .isOpen(quiz.getIsOpen() != null ? quiz.getIsOpen() : false)
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passScore(quiz.getPassScore())
                .maxAttempts(quiz.getMaxAttempts())
                .numberOfQuestions(quiz.getNumberOfQuestions())
                .questionOrder(quiz.getQuestionOrder())
                .showAnswerAfterSubmit(quiz.getShowAnswerAfterSubmit())
                .createdByName(quiz.getUser() != null ? quiz.getUser().getFullName() : null)
                .createdAt(quiz.getCreatedAt())
                .totalQuestions(quizQuestions.size())
                .hasAttempts(hasAttempts)
                .questions(questionDTOs)
                .build();
    }

    private TeacherQuestionDTO toTeacherQuestionDTO(Question question) {
        List<AnswerOption> opts = answerOptionRepository.findByQuestionQuestionId(question.getQuestionId());
        List<TeacherQuestionDTO.AnswerOptionSimpleDTO> optDTOs = opts.stream()
                .map(o -> TeacherQuestionDTO.AnswerOptionSimpleDTO.builder()
                        .answerOptionId(o.getAnswerOptionId())
                        .title(o.getTitle())
                        .correctAnswer(o.getCorrectAnswer())
                        .orderIndex(o.getOrderIndex())
                        .matchTarget(o.getMatchTarget())
                        .build())
                .collect(Collectors.toList());

        return TeacherQuestionDTO.builder()
                .questionId(question.getQuestionId())
                .content(question.getContent())
                .questionType(question.getQuestionType())
                .skill(question.getSkill())
                .cefrLevel(question.getCefrLevel())
                .topic(question.getTopic())
                .tags(question.getTags())
                .explanation(question.getExplanation())
                .audioUrl(question.getAudioUrl())
                .imageUrl(question.getImageUrl())
                .status(question.getStatus())
                .source(question.getSource())
                .createdByName(question.getUser() != null ? question.getUser().getFullName() : null)
                .createdAt(question.getCreatedAt())
                .options(optDTOs)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DTOs
    // ═══════════════════════════════════════════════════════════════════════

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TeacherQuizDTO {
        private Integer quizId;
        private String title;
        private String description;
        private String quizCategory;
        private Integer classId;
        private String className;
        private Integer courseId;
        private String courseName;
        private String status;
        private Boolean isOpen; // Teacher mở/đóng quiz cho học sinh
        private Integer timeLimitMinutes;
        private java.math.BigDecimal passScore;
        private Integer maxAttempts;
        private Integer numberOfQuestions;
        private String questionOrder;
        private Boolean showAnswerAfterSubmit;
        private String createdByName;
        private java.time.LocalDateTime createdAt;
        private int totalQuestions;
        private boolean hasAttempts;
        private List<QuizQuestionSimpleDTO> questions;

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        @lombok.Builder
        public static class QuizQuestionSimpleDTO {
            private Integer quizQuestionId;
            private Integer questionId;
            private String content;
            private String questionType;
            private String skill;
            private String cefrLevel;
            private String status;
            private String source;
            private Integer orderIndex;
            private java.math.BigDecimal points;
            private List<AnswerOptionSimpleDTO> options;
        }

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        @lombok.Builder
        public static class AnswerOptionSimpleDTO {
            private Integer answerOptionId;
            private String title;
            private Boolean correctAnswer;
            private Integer orderIndex;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TeacherQuestionDTO {
        private Integer questionId;
        private String content;
        private String questionType;
        private String skill;
        private String cefrLevel;
        private String topic;
        private String tags;
        private String explanation;
        private String audioUrl;
        private String imageUrl;
        private String status;   // PENDING_REVIEW, PUBLISHED, ARCHIVED
        private String source;    // TEACHER_PRIVATE
        private String createdByName;
        private java.time.LocalDateTime createdAt;
        private List<AnswerOptionSimpleDTO> options;

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        @lombok.Builder
        public static class AnswerOptionSimpleDTO {
            private Integer answerOptionId;
            private String title;
            private Boolean correctAnswer;
            private Integer orderIndex;
            private String matchTarget;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class QuestionBankSimpleDTO {
        private Integer questionId;
        private String content;
        private String questionType;
        private String skill;
        private String cefrLevel;
        private String topic;
        private String status;
        private Integer groupId;
        private String groupContent;
        private List<AnswerOptionSimpleDTO> options;

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        @lombok.Builder
        public static class AnswerOptionSimpleDTO {
            private Integer answerOptionId;
            private String title;
            private Boolean correctAnswer;
            private Integer orderIndex;
            private String matchTarget;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AI IMPORT (Teacher)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Import câu hỏi AI đã chọn vào quiz.
     * Tạo câu hỏi dạng TEACHER_PRIVATE (PENDING_REVIEW) rồi add vào quiz.
     */
    @Transactional
    public ResponseData<AIImportResultDTO> importAIQuestions(AIImportRequestDTO request, String email) {
        try {
            if (request.getQuizId() == null) {
                return ResponseData.error(400, "quizId không được để trống");
            }
            if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
                return ResponseData.error(400, "Danh sách câu hỏi trống");
            }

            Quiz quiz = quizRepository.findById(request.getQuizId()).orElse(null);
            if (quiz == null) return ResponseData.error(404, "Không tìm thấy quiz");

            User teacher = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            List<Integer> importedIds = new ArrayList<>();
            int addedCount = 0;

            for (AIImportRequestDTO.AIQuestionDTO q : request.getQuestions()) {
                // Validate
                if (q.getContent() == null || q.getContent().isBlank()) continue;
                if (q.getQuestionType() == null || !VALID_QUESTION_TYPES.contains(q.getQuestionType())) continue;
                if (q.getSkill() == null || !VALID_SKILLS.contains(q.getSkill())) continue;
                if (q.getCefrLevel() == null || !VALID_CEFR.contains(q.getCefrLevel())) continue;

                // Validate options for choice types
                boolean isNoOptions = NO_OPTIONS_TYPES.contains(q.getQuestionType());
                if (!isNoOptions && (q.getOptions() == null || q.getOptions().isEmpty())) continue;

                // Create question
                Question question = Question.builder()
                        .content(q.getContent())
                        .questionType(q.getQuestionType())
                        .skill(q.getSkill())
                        .cefrLevel(q.getCefrLevel())
                        .topic(q.getTopic())
                        .explanation(q.getExplanation())
                        .audioUrl(q.getAudioUrl())
                        .imageUrl(q.getImageUrl())
                        .status("DRAFT")
                        .source("TEACHER_PRIVATE")
                        .user(teacher)
                        .build();
                questionRepository.save(question);
                importedIds.add(question.getQuestionId());

                // Save answer options
                if (q.getOptions() != null) {
                    for (int i = 0; i < q.getOptions().size(); i++) {
                        AIImportRequestDTO.AIOptionDTO optDto = q.getOptions().get(i);
                        if (optDto.getTitle() == null || optDto.getTitle().isBlank()) continue;

                        // For FILL_IN_BLANK, single correct answer
                        if ("FILL_IN_BLANK".equals(q.getQuestionType())) {
                            // All options are potential correct answers (trimmed)
                            AnswerOption opt = AnswerOption.builder()
                                    .question(question)
                                    .title(optDto.getTitle().trim())
                                    .correctAnswer(Boolean.TRUE.equals(optDto.getCorrect()))
                                    .orderIndex(i)
                                    .build();
                            answerOptionRepository.save(opt);
                        } else if ("MATCHING".equals(q.getQuestionType())) {
                            // matchTarget comes from correctPairs mapping
                            // Build matchTarget from index mapping
                            String matchTarget = null;
                            if (q.getCorrectPairs() != null && q.getMatchRight() != null && q.getMatchLeft() != null) {
                                for (int pairIdx = 0; pairIdx < q.getCorrectPairs().size(); pairIdx++) {
                                    // According to prompt: matchLeft[pairIdx] matches matchRight[correctPairs[pairIdx] - 1]
                                    int leftIdx = pairIdx;
                                    int rightIdx = q.getCorrectPairs().get(pairIdx) - 1;

                                    if (leftIdx < q.getMatchLeft().size() && optDto.getTitle().equals(q.getMatchLeft().get(leftIdx))) {
                                        if (rightIdx >= 0 && rightIdx < q.getMatchRight().size()) {
                                            matchTarget = q.getMatchRight().get(rightIdx);
                                        }
                                        break; // Found the match, no need to continue looping pairs
                                    }
                                }
                            }
                            AnswerOption opt = AnswerOption.builder()
                                    .question(question)
                                    .title(optDto.getTitle())
                                    .correctAnswer(Boolean.TRUE.equals(optDto.getCorrect()))
                                    .orderIndex(i)
                                    .matchTarget(matchTarget)
                                    .build();
                            answerOptionRepository.save(opt);
                        } else {
                            // MULTIPLE_CHOICE_*
                            AnswerOption opt = AnswerOption.builder()
                                    .question(question)
                                    .title(optDto.getTitle())
                                    .correctAnswer(Boolean.TRUE.equals(optDto.getCorrect()))
                                    .orderIndex(i)
                                    .build();
                            answerOptionRepository.save(opt);
                        }
                    }
                }

                // Add to quiz (if question was saved successfully)
                if (!quizQuestionRepository.existsByQuizQuizIdAndQuestionQuestionId(quiz.getQuizId(), question.getQuestionId())) {
                    int currentCount = quizQuestionRepository.countByQuizQuizId(quiz.getQuizId());
                    QuizQuestion qq = QuizQuestion.builder()
                            .quiz(quiz)
                            .question(question)
                            .orderIndex(currentCount + 1)
                            .points(BigDecimal.ONE)
                            .build();
                    quizQuestionRepository.save(qq);
                    addedCount++;
                }
            }

            AIImportResultDTO result = AIImportResultDTO.builder()
                    .imported(addedCount)
                    .questionIds(importedIds)
                    .build();

            return ResponseData.success("Đã tạo " + addedCount + " câu hỏi và thêm vào quiz", result);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  REQUEST / RESPONSE DTOs
    // ═══════════════════════════════════════════════════════════════════════

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AIImportRequestDTO {
        private Integer quizId;
        private List<AIQuestionDTO> questions;

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        @lombok.Builder
        public static class AIQuestionDTO {
            private String content;
            private String questionType;
            private String skill;
            private String cefrLevel;
            private String topic;
            private String explanation;
            private String audioUrl;
            private String imageUrl;
            private List<AIOptionDTO> options;
            private String correctAnswer;
            private List<String> matchLeft;
            private List<String> matchRight;
            private List<Integer> correctPairs;
        }

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        @lombok.Builder
        public static class AIOptionDTO {
            private String title;
            private Boolean correct;
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AIImportResultDTO {
        private int imported;
        private List<Integer> questionIds;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NOTIFICATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Notify enrolled students when a quiz is published or its open status changes.
     */
    private void notifyStudentsQuizPublished(Quiz quiz, Boolean opened, String action) {
        if (quiz == null) return;
        String quizTitle = quiz.getTitle() != null ? quiz.getTitle() : "";
        String className = "";
        String deadline = "";

        if (quiz.getDeadline() != null) {
            deadline = quiz.getDeadline().toString();
        }

        if (quiz.getClazz() != null) {
            className = quiz.getClazz().getClassName() != null ? quiz.getClazz().getClassName() : "";
            List<Registration> regs = registrationRepository.findByClazz_ClassIdAndStatus(quiz.getClazz().getClassId(), "Approved");
            for (Registration reg : regs) {
                User student = reg.getUser();
                if (student == null) continue;
                String studentName = student.getFullName() != null ? student.getFullName() : "";
                String email = student.getEmail();

                if (email != null && !email.isBlank()) {
                    if ("PUBLISHED".equals(action)) {
                        emailService.sendQuizPublishedEmail(email, studentName, quizTitle, className, deadline);
                    }
                }
                if (student.getUserId() != null) {
                    if ("PUBLISHED".equals(action)) {
                        notificationService.sendQuizPublished(Long.valueOf(student.getUserId()), quizTitle, className);
                    }
                }
            }
        }
    }
}
