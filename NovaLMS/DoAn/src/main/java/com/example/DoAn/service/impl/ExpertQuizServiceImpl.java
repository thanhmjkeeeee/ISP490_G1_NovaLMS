package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.AssignmentQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.service.EmailService;
import com.example.DoAn.service.INotificationService;
import com.example.DoAn.dto.response.AssignmentPreviewDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResponseDTO;
import com.example.DoAn.dto.response.SkillSectionSummaryDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.Module;
import com.example.DoAn.model.QuizCategory;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.IExpertQuizService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertQuizServiceImpl implements IExpertQuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAssignmentRepository quizAssignmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ClazzRepository clazzRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final ObjectMapper objectMapper;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;
    private final INotificationService notificationService;
    private final AnswerOptionRepository answerOptionRepository;

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "ENTRY_TEST", "COURSE_QUIZ", "MODULE_QUIZ", "LESSON_QUIZ",
            "COURSE_ASSIGNMENT", "MODULE_ASSIGNMENT");
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private static final Set<String> VALID_ORDERS = Set.of("FIXED", "RANDOM");

    // ─── CREATE QUIZ ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizResponseDTO createQuiz(QuizRequestDTO request, String email) throws JsonProcessingException {
        User expert = findExpert(email);
        validateQuizRequest(request);

        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .quizCategory(request.getQuizCategory())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passScore(request.getPassScore())
                .maxAttempts(request.getMaxAttempts())
                .numberOfQuestions(request.getNumberOfQuestions())
                .questionOrder(request.getQuestionOrder() != null ? request.getQuestionOrder() : "FIXED")
                .showAnswerAfterSubmit(
                        request.getShowAnswerAfterSubmit() != null ? request.getShowAnswerAfterSubmit() : false)
                .isHybridEnabled(request.getIsHybridEnabled() != null ? request.getIsHybridEnabled() : false)
                .targetSkill(request.getTargetSkill())
                .user(expert)
                .openAt(request.getOpenAt())
                .closeAt(request.getCloseAt())
                .deadline(request.getDeadline())
                .build();

        // Set sequential + skill fields for COURSE_QUIZ and assignment types
        QuizCategory cat = QuizCategory.fromValue(request.getQuizCategory());
        if ("COURSE_QUIZ".equals(request.getQuizCategory()) || (cat != null && cat.isAssignment())) {
            quiz.setIsSequential(true);
            quiz.setSkillOrder("[\"LISTENING\",\"READING\",\"SPEAKING\",\"WRITING\"]");
        }
        if (request.getTimeLimitPerSkill() != null) {
            quiz.setTimeLimitPerSkill(objectMapper.writeValueAsString(request.getTimeLimitPerSkill()));
        }

        // Gắn course nếu là COURSE_QUIZ hoặc COURSE_ASSIGNMENT
        if (("COURSE_QUIZ".equals(request.getQuizCategory()) || "COURSE_ASSIGNMENT".equals(request.getQuizCategory()))
                && request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy khóa học với ID: " + request.getCourseId()));
            quiz.setCourse(course);
        }

        // Gắn module nếu có
        if (request.getModuleId() != null) {
            Module module = moduleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy chương với ID: " + request.getModuleId()));
            quiz.setModule(module);
            if (quiz.getCourse() == null && module.getCourse() != null) {
                quiz.setCourse(module.getCourse());
            }
        }

        // Gắn lesson nếu có
        if (request.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy bài học với ID: " + request.getLessonId()));
            quiz.setLesson(lesson);
            if (quiz.getModule() == null && lesson.getModule() != null) {
                quiz.setModule(lesson.getModule());
            }
            if (quiz.getCourse() == null && quiz.getModule() != null && quiz.getModule().getCourse() != null) {
                quiz.setCourse(quiz.getModule().getCourse());
            }
        }

        // Gắn class nếu teacher tạo quiz từ class-sessions
        if (request.getClassId() != null) {
            Clazz clazz = clazzRepository.findById(request.getClassId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Không tìm thấy lớp với ID: " + request.getClassId()));
            quiz.setClazz(clazz);
            // Tự động lấy course từ lớp nếu chưa có
            if (quiz.getCourse() == null && clazz.getCourse() != null) {
                quiz.setCourse(clazz.getCourse());
            }
        }

        quizRepository.save(quiz);

        // Tạo QuizAssignment để quiz xuất hiện trong danh sách quiz của module/lesson
        if (quiz.getLesson() != null) {
            int maxOrder = quizAssignmentRepository
                    .findByLesson_LessonIdOrderByOrderIndexAsc(quiz.getLesson().getLessonId())
                    .stream()
                    .mapToInt(QuizAssignment::getOrderIndex)
                    .max()
                    .orElse(0);

            QuizAssignment assignment = QuizAssignment.builder()
                    .quiz(quiz)
                    .lesson(quiz.getLesson())
                    .module(quiz.getModule())
                    .orderIndex(maxOrder + 1)
                    .build();
            quizAssignmentRepository.save(assignment);
        } else if (quiz.getModule() != null) {
            int maxOrder = quizAssignmentRepository
                    .findByModule_ModuleIdOrderByOrderIndexAsc(quiz.getModule().getModuleId())
                    .stream()
                    .mapToInt(QuizAssignment::getOrderIndex)
                    .max()
                    .orElse(0);

            QuizAssignment assignment = QuizAssignment.builder()
                    .module(quiz.getModule())
                    .quiz(quiz)
                    .orderIndex(maxOrder + 1)
                    .build();
            quizAssignmentRepository.save(assignment);
        }

        return toResponseDTO(quiz);
    }

    // ─── UPDATE QUIZ ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizResponseDTO updateQuiz(Integer quizId, QuizRequestDTO request, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        // Kiểm tra đã có học viên làm chưa
        // Cho phép cập nhật nếu quiz đang ở Draft HOẶC đang chuyển về Draft
        if (hasStudentAttempts(quizId) && !"DRAFT".equals(quiz.getStatus()) && !"DRAFT".equals(request.getStatus())) {
            throw new InvalidDataException(
                    "Không thể cập nhật cấu hình quiz đã có học viên làm bài. Hãy chuyển trạng thái về Draft trước.");
        }

        // Lấy lại danh mục và courseId nếu update không gửi lên
        if (request.getQuizCategory() == null) {
            request.setQuizCategory(quiz.getQuizCategory());
        }
        if (request.getCourseId() == null && quiz.getCourse() != null) {
            request.setCourseId(quiz.getCourse().getCourseId());
        }

        validateQuizRequest(request);

        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quiz.setPassScore(request.getPassScore());
        quiz.setMaxAttempts(request.getMaxAttempts());

        if (request.getNumberOfQuestions() != null) {
            quiz.setNumberOfQuestions(request.getNumberOfQuestions());
        }

        if (request.getQuestionOrder() != null) {
            quiz.setQuestionOrder(request.getQuestionOrder());
        }
        if (request.getShowAnswerAfterSubmit() != null) {
            quiz.setShowAnswerAfterSubmit(request.getShowAnswerAfterSubmit());
        }
        if (request.getIsHybridEnabled() != null) {
            quiz.setIsHybridEnabled(request.getIsHybridEnabled());
        }
        quiz.setTargetSkill(request.getTargetSkill());
        if (request.getStatus() != null) {
            quiz.setStatus(request.getStatus());
        }

        if (request.getTimeLimitPerSkill() != null) {
            try {
                quiz.setTimeLimitPerSkill(objectMapper.writeValueAsString(request.getTimeLimitPerSkill()));
            } catch (JsonProcessingException e) {
                throw new InvalidDataException("Lỗi định dạng timeLimitPerSkill");
            }
        }

        // Cập nhật course nếu thay đổi
        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy khóa học với ID: " + request.getCourseId()));
            quiz.setCourse(course);
        }

        // Cập nhật module & lesson nếu thay đổi
        if (request.getModuleId() != null) {
            Module module = moduleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy chương với ID: " + request.getModuleId()));
            quiz.setModule(module);
            if (quiz.getCourse() == null)
                quiz.setCourse(module.getCourse());
        }
        if (request.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(request.getLessonId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy bài học với ID: " + request.getLessonId()));
            quiz.setLesson(lesson);
            if (quiz.getModule() == null)
                quiz.setModule(lesson.getModule());
            if (quiz.getCourse() == null && quiz.getModule() != null)
                quiz.setCourse(quiz.getModule().getCourse());
        }

        // Cập nhật schedule & sequential logic
        quiz.setOpenAt(request.getOpenAt());
        quiz.setCloseAt(request.getCloseAt());
        quiz.setDeadline(request.getDeadline());

        if (request.getIsSequential() != null) {
            quiz.setIsSequential(request.getIsSequential());
        }
        if (request.getSkillOrder() != null) {
            try {
                quiz.setSkillOrder(objectMapper.writeValueAsString(request.getSkillOrder()));
            } catch (JsonProcessingException e) {
                throw new InvalidDataException("Lỗi định dạng skillOrder");
            }
        }

        quizRepository.save(quiz);
        return toResponseDTO(quiz);
    }

    // ─── DELETE QUIZ ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteQuiz(Integer quizId, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        if (hasStudentAttempts(quizId)) {
            throw new InvalidDataException("Không thể xóa quiz đã có học viên làm bài.");
        }

        quizRepository.delete(quiz);
    }

    // ─── GET BY ID ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public QuizResponseDTO getQuizById(Integer quizId) {
        Quiz quiz = findQuiz(quizId);
        return toResponseDTO(quiz);
    }

    // ─── LIST + FILTER ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuizResponseDTO> getQuizzes(
            Integer courseId, String category, String status, String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Quiz> pageResult = quizRepository.findByFilters(courseId, category, status, keyword, pageable);

        List<QuizResponseDTO> items = pageResult.getContent().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());

        return PageResponse.<QuizResponseDTO>builder()
                .items(items)
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .last(pageResult.isLast())
                .build();
    }

    // ─── CHANGE STATUS ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizResponseDTO changeStatus(Integer quizId, String newStatus, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        if (!VALID_STATUSES.contains(newStatus)) {
            throw new InvalidDataException("Trạng thái không hợp lệ: " + newStatus);
        }

        String currentStatus = quiz.getStatus();
        boolean validTransition = ("DRAFT".equals(currentStatus) && "PUBLISHED".equals(newStatus)) ||
                ("PUBLISHED".equals(currentStatus) && "ARCHIVED".equals(newStatus)) ||
                ("PUBLISHED".equals(currentStatus) && "DRAFT".equals(newStatus));

        if (!validTransition) {
            throw new InvalidDataException(
                    "Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
        }

        // Publish cần ít nhất 1 câu hỏi và đủ số lượng câu hỏi
        if ("PUBLISHED".equals(newStatus)) {
            int questionCount = quizQuestionRepository.countByQuizQuizId(quizId);
            if (questionCount == 0) {
                throw new InvalidDataException("Không thể xuất bản quiz chưa có câu hỏi nào.");
            }
            if (quiz.getNumberOfQuestions() != null && questionCount < quiz.getNumberOfQuestions()) {
                throw new InvalidDataException("Số lượng câu hỏi chưa đủ (" + questionCount + "/"
                        + quiz.getNumberOfQuestions() + "). Chờ thêm câu hỏi để xuất bản.");
            }

            // COURSE_ASSIGNMENT: bắt buộc phải có đủ 4 kỹ năng
            if ("COURSE_ASSIGNMENT".equals(quiz.getQuizCategory())) {
                List<String> requiredSkills = Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING");
                List<String> missingSkills = new java.util.ArrayList<>();
                for (String skill : requiredSkills) {
                    long count = quizQuestionRepository.countByQuizIdAndSkill(quizId, skill);
                    if (count == 0) {
                        missingSkills.add(skill);
                    }
                }
                if (!missingSkills.isEmpty()) {
                    throw new InvalidDataException(
                        "Assignment phải có đủ 4 kỹ năng (Listening, Reading, Speaking, Writing) mới được xuất bản. "
                        + "Còn thiếu: " + String.join(", ", missingSkills));
                }
            }
        }

        quiz.setStatus(newStatus);
        quizRepository.save(quiz);

        // ── Notify on publish ─────────────────────────────────────────────────
        if ("PUBLISHED".equals(newStatus)) {
            notifyOnPublish(quiz);
        }

        return toResponseDTO(quiz);
    }

    @Override
    @Transactional
    public QuizResponseDTO addQuestionToQuiz(Integer quizId, QuizQuestionRequestDTO request, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        // Ưu tiên sử dụng itemType nếu có gửi từ Frontend
        if ("GROUP".equalsIgnoreCase(request.getItemType())) {
            QuestionGroup group = questionGroupRepository.findById(request.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy bộ câu hỏi với ID: " + request.getQuestionId()));
            return addGroupToQuiz(quiz, group, request);
        } else if ("SINGLE".equalsIgnoreCase(request.getItemType())) {
            Question question = questionRepository.findById(request.getQuestionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy câu hỏi với ID: " + request.getQuestionId()));
            return addSingleQuestionToQuiz(quiz, question, request);
        }

        // Fallback: Tự đoán nêú không có itemType (như code cũ)
        Optional<Question> loneQ = questionRepository.findById(request.getQuestionId());
        if (loneQ.isPresent()) {
            return addSingleQuestionToQuiz(quiz, loneQ.get(), request);
        }

        Optional<QuestionGroup> groupQ = questionGroupRepository.findById(request.getQuestionId());
        if (groupQ.isPresent()) {
            return addGroupToQuiz(quiz, groupQ.get(), request);
        }

        throw new ResourceNotFoundException("Không tìm thấy dữ liệu với ID: " + request.getQuestionId());
    }

    private QuizResponseDTO addSingleQuestionToQuiz(Quiz quiz, Question question, QuizQuestionRequestDTO request) {
        if (!"PUBLISHED".equals(question.getStatus())) {
            throw new InvalidDataException("Chỉ có thể thêm câu hỏi đã Published vào quiz.");
        }

        validateQuestionForQuiz(quiz, question);

        if (quizQuestionRepository.existsByQuizQuizIdAndQuestionQuestionId(quiz.getQuizId(),
                question.getQuestionId())) {
            throw new InvalidDataException("Câu hỏi này đã có trong quiz.");
        }

        int currentCount = quizQuestionRepository.countByQuizQuizId(quiz.getQuizId());
        QuizQuestion qq = QuizQuestion.builder()
                .quiz(quiz)
                .question(question)
                .questionGroup(question.getQuestionGroup())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : currentCount + 1)
                .points(request.getPoints() != null ? request.getPoints() : BigDecimal.ONE)
                .build();

        quizQuestionRepository.save(qq);
        return toResponseDTO(quiz);
    }

    private QuizResponseDTO addGroupToQuiz(Quiz quiz, QuestionGroup group, QuizQuestionRequestDTO request) {
        if (!"PUBLISHED".equals(group.getStatus())) {
            throw new InvalidDataException("Chỉ có thể thêm bộ câu hỏi đã Published vào quiz.");
        }

        List<Question> subQuestions = group.getQuestions();
        if (subQuestions == null || subQuestions.isEmpty()) {
            throw new InvalidDataException("Bộ câu hỏi này không có câu hỏi con nào.");
        }

        int currentCount = quizQuestionRepository.countByQuizQuizId(quiz.getQuizId());
        for (int i = 0; i < subQuestions.size(); i++) {
            Question q = subQuestions.get(i);
            if (quizQuestionRepository.existsByQuizQuizIdAndQuestionQuestionId(quiz.getQuizId(), q.getQuestionId())) {
                continue; // Đã có thì bỏ qua
            }

            QuizQuestion qq = QuizQuestion.builder()
                    .quiz(quiz)
                    .question(q)
                    .questionGroup(group)
                    .orderIndex(currentCount + i + 1)
                    .points(BigDecimal.ONE)
                    .build();
            quizQuestionRepository.save(qq);
        }

        return toResponseDTO(quiz);
    }

    private void validateQuestionForQuiz(Quiz quiz, Question question) {
        if ("ENTRY_TEST".equals(quiz.getQuizCategory()) && !Boolean.TRUE.equals(quiz.getIsHybridEnabled())) {
            String qType = question.getQuestionType();
            if (!("MULTIPLE_CHOICE_SINGLE".equals(qType) ||
                    "MULTIPLE_CHOICE_MULTI".equals(qType) ||
                    "MATCHING".equals(qType))) {
                throw new InvalidDataException(
                        "ENTRY_TEST chỉ cấu hình được những câu hỏi là multiple choices, matching.");
            }
        }

        if (Boolean.TRUE.equals(quiz.getIsHybridEnabled()) && quiz.getTargetSkill() != null) {
            if (!quiz.getTargetSkill().equals(question.getSkill())) {
                throw new InvalidDataException("Quiz kỹ năng [" + quiz.getTargetSkill() + "] không khớp với câu hỏi ["
                        + question.getSkill() + "].");
            }
        }
    }

    @Override
    @Transactional
    public QuizResponseDTO removeQuestionFromQuiz(Integer quizId, Integer questionId, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        if (!quizQuestionRepository.existsByQuizQuizIdAndQuestionQuestionId(quizId, questionId)) {
            throw new ResourceNotFoundException("Câu hỏi không tồn tại trong quiz này.");
        }

        quizQuestionRepository.deleteByQuizQuizIdAndQuestionQuestionId(quizId, questionId);
        return toResponseDTO(quiz);
    }

    @Override
    @Transactional
    public QuizResponseDTO removeGroupFromQuiz(Integer quizId, Integer groupId, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        quizQuestionRepository.deleteByQuizQuizIdAndQuestionGroupGroupId(quizId, groupId);
        return toResponseDTO(quiz);
    }

    @Override
    @Transactional
    public QuizResponseDTO reorderQuestions(Integer quizId, List<QuizQuestionRequestDTO> orderedList, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        List<QuizQuestion> existingQuestions = quizQuestionRepository.findByQuizQuizIdOrderByOrderIndexAsc(quizId);

        for (QuizQuestionRequestDTO item : orderedList) {
            existingQuestions.stream()
                    .filter(qq -> qq.getQuestion().getQuestionId().equals(item.getQuestionId()))
                    .findFirst()
                    .ifPresent(qq -> {
                        qq.setOrderIndex(item.getOrderIndex());
                        if (item.getPoints() != null) {
                            qq.setPoints(item.getPoints());
                        }
                    });
        }

        quizQuestionRepository.saveAll(existingQuestions);
        return toResponseDTO(quiz);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private User findExpert(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia với email: " + email));
    }

    private Quiz findQuiz(Integer quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quiz với ID: " + quizId));
    }

    private boolean hasStudentAttempts(Integer quizId) {
        // Chỉ coi là đã có học sinh làm nếu có bài đã nộp hoặc đang/đã chấm
        // Tránh việc bị khóa sửa do các bản ghi rác hoặc đang làm dở
        return quizResultRepository.existsByQuizQuizIdAndStatusIn(quizId, List.of("SUBMITTED", "GRADING", "GRADED"));
    }

    private void validateQuizRequest(QuizRequestDTO request) {
        if (!VALID_CATEGORIES.contains(request.getQuizCategory())) {
            throw new InvalidDataException("Loại quiz không hợp lệ: " + request.getQuizCategory());
        }

        // Kiểm tra validation cho Assignment
        QuizCategory cat = QuizCategory.fromValue(request.getQuizCategory());
        if (cat != null && cat.isAssignment()) {
            if (request.getTimeLimitPerSkill() == null || request.getTimeLimitPerSkill().isEmpty()) {
                throw new InvalidDataException(
                        "Bài tập lớn (Assignment) bắt buộc phải thiết lập thời gian cho từng kỹ năng (Listening, Reading, Speaking, Writing).");
            }
        }

        if (Set.of("COURSE_QUIZ").contains(request.getQuizCategory()) && request.getCourseId() == null) {
            throw new InvalidDataException("Course Quiz phải gắn với một khóa học (courseId bắt buộc).");
        }
        if (request.getQuestionOrder() != null && !VALID_ORDERS.contains(request.getQuestionOrder())) {
            throw new InvalidDataException("Thứ tự câu hỏi không hợp lệ: " + request.getQuestionOrder());
        }
        if (request.getPassScore() != null) {
            if (request.getPassScore().compareTo(BigDecimal.ZERO) < 0 ||
                    request.getPassScore().compareTo(new BigDecimal("100")) > 0) {
                throw new InvalidDataException("Điểm đạt phải từ 0 đến 100%.");
            }
        }
        if (request.getStatus() != null && !VALID_STATUSES.contains(request.getStatus())) {
            throw new InvalidDataException("Trạng thái không hợp lệ: " + request.getStatus());
        }
    }

    private QuizResponseDTO toResponseDTO(Quiz quiz) {
        List<QuizQuestion> quizQuestions = quizQuestionRepository
                .findByQuizQuizIdOrderByOrderIndexAsc(quiz.getQuizId());
        boolean hasAttempts = hasStudentAttempts(quiz.getQuizId());

        List<QuizResponseDTO.QuizQuestionResponseDTO> questionDTOs = quizQuestions.stream()
                .map(qq -> {
                    Question q = qq.getQuestion();
                    return QuizResponseDTO.QuizQuestionResponseDTO.builder()
                            .quizQuestionId(qq.getQuizQuestionId())
                            .questionId(q.getQuestionId())
                            .groupId(qq.getQuestionGroup() != null ? qq.getQuestionGroup().getGroupId() : null)
                            .groupContent(
                                    qq.getQuestionGroup() != null ? qq.getQuestionGroup().getGroupContent() : null)
                            .questionContent(q.getContent())
                            .questionType(q.getQuestionType())
                            .skill(q.getSkill())
                            .cefrLevel(q.getCefrLevel())
                            .orderIndex(qq.getOrderIndex())
                            .points(qq.getPoints())
                            .build();
                })
                .collect(Collectors.toList());

        long regCount = 0;
        if (quiz.getCourse() != null) {
            regCount = registrationRepository.countByCourse_CourseId(quiz.getCourse().getCourseId());
        }

        return QuizResponseDTO.builder()
                .quizId(quiz.getQuizId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .quizCategory(quiz.getQuizCategory())
                .courseId(quiz.getCourse() != null ? quiz.getCourse().getCourseId() : null)
                .courseName(quiz.getCourse() != null ? quiz.getCourse().getCourseName() : null)
                .moduleId(quiz.getModule() != null ? quiz.getModule().getModuleId() : null)
                .moduleName(quiz.getModule() != null ? quiz.getModule().getModuleName() : null)
                .lessonId(quiz.getLesson() != null ? quiz.getLesson().getLessonId() : null)
                .lessonName(quiz.getLesson() != null ? quiz.getLesson().getLessonName() : null)
                .status(quiz.getStatus())
                .isOpen(quiz.getIsOpen() != null ? quiz.getIsOpen() : false)
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passScore(quiz.getPassScore())
                .maxAttempts(quiz.getMaxAttempts())
                .numberOfQuestions(quiz.getNumberOfQuestions())
                .questionOrder(quiz.getQuestionOrder())
                .showAnswerAfterSubmit(quiz.getShowAnswerAfterSubmit())
                .isHybridEnabled(quiz.getIsHybridEnabled() != null ? quiz.getIsHybridEnabled() : false)
                .targetSkill(quiz.getTargetSkill())
                .createdByName(quiz.getUser() != null ? quiz.getUser().getFullName() : null)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .totalQuestions((int) (quizQuestions.stream()
                        .map(qq -> qq.getQuestionGroup() != null ? "G" + qq.getQuestionGroup().getGroupId()
                                : "Q" + qq.getQuestion().getQuestionId())
                        .distinct()
                        .count()))
                .hasAttempts(hasAttempts)
                .registrationCount(regCount)
                .timeLimitPerSkill(quiz.getTimeLimitPerSkill())
                .openAt(quiz.getOpenAt() != null
                        ? quiz.getOpenAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                        : null)
                .closeAt(quiz.getCloseAt() != null
                        ? quiz.getCloseAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                        : null)
                .deadline(quiz.getDeadline() != null
                        ? quiz.getDeadline().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                        : null)
                .isSequential(quiz.getIsSequential())
                .skillOrder(quiz.getSkillOrder())
                .questions(questionDTOs)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ASSIGNMENT OPERATIONS (4-skill sequential)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Map<String, SkillSectionSummaryDTO> getSkillSummaries(Integer quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz not found");
        }
        java.util.LinkedHashMap<String, SkillSectionSummaryDTO> result = new java.util.LinkedHashMap<>();
        List<String> skills = Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING");
        for (String skill : skills) {
            long count = quizQuestionRepository.countByQuizIdAndSkill(quizId, skill);
            result.put(skill, new SkillSectionSummaryDTO(skill, count, 0L,
                    count > 0 ? "READY" : "DRAFT"));
        }
        return result;
    }

    @Override
    public void addQuestionsToSection(Integer quizId, AssignmentQuestionRequestDTO dto, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);
        if (!Boolean.TRUE.equals(quiz.getIsSequential())) {
            throw new InvalidDataException("This quiz does not support section-based question addition");
        }
        String skill = dto.getSkill();
        List<String> validSkills = Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING");
        if (!validSkills.contains(skill)) {
            throw new InvalidDataException("Kỹ năng không hợp lệ: " + skill);
        }
        List<QuizQuestion> existing = quizQuestionRepository.findByQuizQuizIdAndSkill(quizId, skill);
        Set<Integer> existingIds = new java.util.HashSet<>();
        for (QuizQuestion qq : existing)
            existingIds.add(qq.getQuestion().getQuestionId());
        int nextOrder = existing.size() + 1;
        for (Integer questionId : dto.getQuestionIds()) {
            if (existingIds.contains(questionId))
                continue;
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
            QuizQuestion qq = QuizQuestion.builder()
                    .quiz(quiz).question(question).skill(skill)
                    .orderIndex(nextOrder++).points(BigDecimal.ONE)
                    .build();
            quizQuestionRepository.save(qq);
        }
    }

    @Override
    public void removeQuestion(Integer quizId, Integer questionId) {
        quizQuestionRepository.findByQuizQuizIdAndQuestionQuestionId(quizId, questionId)
                .ifPresent(quizQuestionRepository::delete);
    }

    @Override
    public QuizResponseDTO publishAssignment(Integer quizId) {
        Quiz quiz = findQuiz(quizId);
        if (!"DRAFT".equals(quiz.getStatus())) {
            throw new InvalidDataException("Only DRAFT quizzes can be published");
        }

        String cat = quiz.getQuizCategory();

        // COURSE_ASSIGNMENT: bắt buộc phải có đủ 4 kỹ năng mới cho xuất bản
        if ("COURSE_ASSIGNMENT".equals(cat)) {
            // Kiểm tra per-skill time limits
            if (quiz.getTimeLimitPerSkill() == null || quiz.getTimeLimitPerSkill().trim().isEmpty()
                    || "{}".equals(quiz.getTimeLimitPerSkill().trim())) {
                throw new InvalidDataException("COURSE_ASSIGNMENT requires per-skill time limits to be set.");
            }

            // Kiểm tra đủ 4 kỹ năng
            List<String> requiredSkills = Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING");
            List<String> missingSkills = new java.util.ArrayList<>();
            for (String skill : requiredSkills) {
                long count = quizQuestionRepository.countByQuizIdAndSkill(quizId, skill);
                if (count == 0) {
                    missingSkills.add(skill);
                }
            }
            if (!missingSkills.isEmpty()) {
                throw new InvalidDataException(
                    "Assignment phải có đủ 4 kỹ năng (Listening, Reading, Speaking, Writing) mới được xuất bản. "
                    + "Còn thiếu: " + String.join(", ", missingSkills));
            }
        }

        // ENTRY_TEST: chỉ cần ≥1 câu (đã có check ở changeStatus, không cần thêm)

        quiz.setStatus("PUBLISHED");
        quiz.setIsOpen(false);
        quizRepository.save(quiz);
        notifyOnPublish(quiz);
        return toResponseDTO(quiz);
    }

    // ── Notification helper ────────────────────────────────────────────────────

    private void notifyOnPublish(Quiz quiz) {
        if (quiz == null)
            return;

        String quizTitle = quiz.getTitle() != null ? quiz.getTitle() : "";
        String courseName = quiz.getCourse() != null && quiz.getCourse().getCourseName() != null
                ? quiz.getCourse().getCourseName()
                : "";
        String className = quiz.getClazz() != null ? quiz.getClazz().getClassName() : "";
        String deadline = quiz.getDeadline() != null ? quiz.getDeadline().toString() : "";

        // For COURSE_QUIZ / COURSE_ASSIGNMENT with class → notify enrolled students
        if (quiz.getClazz() != null) {
            List<Registration> regs = registrationRepository.findApprovedByClassId(quiz.getClazz().getClassId());
            for (Registration reg : regs) {
                User student = reg.getUser();
                if (student == null)
                    continue;
                String studentName = student.getFullName() != null ? student.getFullName() : "";
                String email = student.getEmail();

                boolean isAssignment = "COURSE_ASSIGNMENT".equals(quiz.getQuizCategory())
                        || "MODULE_ASSIGNMENT".equals(quiz.getQuizCategory());

                if (email != null && !email.isBlank()) {
                    if (isAssignment) {
                        emailService.sendAssignmentPublishedEmail(email, studentName, quizTitle, className, deadline);
                    } else {
                        emailService.sendQuizPublishedEmail(email, studentName, quizTitle, className, deadline);
                    }
                }
                if (student.getUserId() != null) {
                    if (isAssignment) {
                        notificationService.sendAssignmentPublished(Long.valueOf(student.getUserId()), quizTitle,
                                className);
                    } else {
                        notificationService.sendQuizPublished(Long.valueOf(student.getUserId()), quizTitle, className);
                    }
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentPreviewDTO getAssignmentPreview(Integer quizId) {
        Quiz quiz = findQuiz(quizId);
        Map<String, SkillSectionSummaryDTO> summaries = getSkillSummaries(quizId);
        List<String> missing = new java.util.ArrayList<>();
        long total = 0;
        for (SkillSectionSummaryDTO s : summaries.values()) {
            if (s.getQuestionCount() == 0) missing.add(s.getSkill());
            total += s.getQuestionCount();
        }
        Map<String, Integer> timeLimits = null;
        if (quiz.getTimeLimitPerSkill() != null) {
            try {
                timeLimits = objectMapper.readValue(quiz.getTimeLimitPerSkill(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {
                        });
            } catch (Exception ignored) {
            }
        }
        return new AssignmentPreviewDTO(
                quiz.getQuizId(), quiz.getTitle(), quiz.getDescription(), quiz.getQuizCategory(),
                new java.util.ArrayList<>(summaries.values()), total,
                quiz.getPassScore() != null ? quiz.getPassScore() : BigDecimal.ZERO,
                timeLimits, quiz.getPassScore(), quiz.getMaxAttempts(),
                quiz.getShowAnswerAfterSubmit(), missing, missing.isEmpty()
        );
    }

    @Override
    @Transactional
    public QuizResponseDTO importAIQuestions(Integer quizId,
            List<com.example.DoAn.dto.response.AIGenerateResponseDTO.QuestionDTO> questions, String passage,
            String audioUrl, String email) {
        User expert = findExpert(email);
        Quiz quiz = findQuiz(quizId);
 
        QuestionGroup sharedGroup = null;
        if (passage != null && !passage.isBlank()) {
            com.example.DoAn.dto.response.AIGenerateResponseDTO.QuestionDTO first = questions.get(0);
            sharedGroup = QuestionGroup.builder()
                    .groupContent(passage)
                    .audioUrl(audioUrl)
                    .skill(first.getSkill())
                    .cefrLevel(first.getCefrLevel())
                    .topic(first.getTopic())
                    .status("PUBLISHED")
                    .user(expert)
                    .build();
            questionGroupRepository.save(sharedGroup);
        }

        int currentCount = quizQuestionRepository.countByQuizQuizId(quizId);
        for (com.example.DoAn.dto.response.AIGenerateResponseDTO.QuestionDTO q : questions) {
            Question question = Question.builder()
                    .content(q.getContent())
                    .questionType(q.getQuestionType())
                    .skill(q.getSkill())
                    .cefrLevel(q.getCefrLevel())
                    .topic(q.getTopic())
                    .explanation(q.getExplanation())
                    .audioUrl(q.getAudioUrl())
                    .imageUrl(q.getImageUrl())
                    .status("PUBLISHED")
                    .source("AI_GENERATED")
                    .user(expert)
                    .questionGroup(sharedGroup)
                    .build();
            questionRepository.save(question);

            // Options
            if (q.getOptions() != null) {
                for (int i = 0; i < q.getOptions().size(); i++) {
                    com.example.DoAn.dto.response.AIGenerateResponseDTO.OptionDTO optDTO = q.getOptions().get(i);
                    AnswerOption opt = AnswerOption.builder()
                            .question(question)
                            .title(optDTO.getTitle())
                            .correctAnswer(Boolean.TRUE.equals(optDTO.getCorrect()))
                            .orderIndex(i + 1)
                            .build();
                    answerOptionRepository.save(opt);
                }
            }

            // Add to Quiz
            QuizQuestion qq = QuizQuestion.builder()
                    .quiz(quiz)
                    .question(question)
                    .questionGroup(sharedGroup)
                    .orderIndex(++currentCount)
                    .points(BigDecimal.ONE)
                    .build();
            quizQuestionRepository.save(qq);
        }

        return toResponseDTO(quiz);
    }
}
