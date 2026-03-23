package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.QuizQuestionRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.QuizResponseDTO;
import com.example.DoAn.exception.InvalidDataException;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.IExpertQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertQuizServiceImpl implements IExpertQuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    private static final Set<String> VALID_CATEGORIES = Set.of("ENTRY_TEST", "COURSE_QUIZ");
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");
    private static final Set<String> VALID_ORDERS = Set.of("FIXED", "RANDOM");

    // ─── CREATE QUIZ ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizResponseDTO createQuiz(QuizRequestDTO request, String email) {
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
                .showAnswerAfterSubmit(request.getShowAnswerAfterSubmit() != null ? request.getShowAnswerAfterSubmit() : false)
                .user(expert)
                .build();

        // Gắn course nếu là COURSE_QUIZ
        if ("COURSE_QUIZ".equals(request.getQuizCategory())) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + request.getCourseId()));
            quiz.setCourse(course);
        }

        quizRepository.save(quiz);
        return toResponseDTO(quiz);
    }

    // ─── UPDATE QUIZ ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public QuizResponseDTO updateQuiz(Integer quizId, QuizRequestDTO request, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        // Kiểm tra đã có học viên làm chưa
        if (hasStudentAttempts(quizId) && !"DRAFT".equals(quiz.getStatus())) {
            throw new InvalidDataException("Không thể cập nhật quiz đã có học viên làm bài. Hãy chuyển về Draft trước.");
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
        if (request.getStatus() != null) {
            quiz.setStatus(request.getStatus());
        }

        // Cập nhật course nếu thay đổi
        if ("COURSE_QUIZ".equals(request.getQuizCategory()) && request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học với ID: " + request.getCourseId()));
            quiz.setCourse(course);
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
        boolean validTransition =
            ("DRAFT".equals(currentStatus) && "PUBLISHED".equals(newStatus)) ||
            ("PUBLISHED".equals(currentStatus) && "ARCHIVED".equals(newStatus)) ||
            ("PUBLISHED".equals(currentStatus) && "DRAFT".equals(newStatus));

        if (!validTransition) {
            throw new InvalidDataException(
                "Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus
            );
        }

        // Publish cần ít nhất 1 câu hỏi và đủ số lượng câu hỏi
        if ("PUBLISHED".equals(newStatus)) {
            int questionCount = quizQuestionRepository.countByQuizQuizId(quizId);
            if (questionCount == 0) {
                throw new InvalidDataException("Không thể xuất bản quiz chưa có câu hỏi nào.");
            }
            if (quiz.getNumberOfQuestions() != null && questionCount < quiz.getNumberOfQuestions()) {
                throw new InvalidDataException("Số lượng câu hỏi chưa đủ (" + questionCount + "/" + quiz.getNumberOfQuestions() + "). Chờ thêm câu hỏi để xuất bản.");
            }
        }

        quiz.setStatus(newStatus);
        quizRepository.save(quiz);
        return toResponseDTO(quiz);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  QUESTION MANAGEMENT WITHIN QUIZ
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public QuizResponseDTO addQuestionToQuiz(Integer quizId, QuizQuestionRequestDTO request, String email) {
        findExpert(email);
        Quiz quiz = findQuiz(quizId);

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Không tìm thấy câu hỏi với ID: " + request.getQuestionId()
                ));

        // Chỉ cho thêm câu hỏi đã Published
        if (!"PUBLISHED".equals(question.getStatus())) {
            throw new InvalidDataException("Chỉ có thể thêm câu hỏi đã Published vào quiz.");
        }

        // Kiểm tra duplicate
        if (quizQuestionRepository.existsByQuizQuizIdAndQuestionQuestionId(quizId, request.getQuestionId())) {
            throw new InvalidDataException("Câu hỏi này đã có trong quiz.");
        }

        int currentCount = quizQuestionRepository.countByQuizQuizId(quizId);

        QuizQuestion quizQuestion = QuizQuestion.builder()
                .quiz(quiz)
                .question(question)
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : currentCount + 1)
                .points(request.getPoints() != null ? request.getPoints() : BigDecimal.ONE)
                .build();

        quizQuestionRepository.save(quizQuestion);
        return toResponseDTO(quiz);
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
    //  PRIVATE HELPERS
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
        return quizResultRepository.countByQuizQuizId(quizId) > 0;
    }

    private void validateQuizRequest(QuizRequestDTO request) {
        if (!VALID_CATEGORIES.contains(request.getQuizCategory())) {
            throw new InvalidDataException("Loại quiz không hợp lệ: " + request.getQuizCategory());
        }
        if ("COURSE_QUIZ".equals(request.getQuizCategory()) && request.getCourseId() == null) {
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
        List<QuizQuestion> quizQuestions = quizQuestionRepository.findByQuizQuizIdOrderByOrderIndexAsc(quiz.getQuizId());
        boolean hasAttempts = hasStudentAttempts(quiz.getQuizId());

        List<QuizResponseDTO.QuizQuestionResponseDTO> questionDTOs = quizQuestions.stream()
                .map(qq -> {
                    Question q = qq.getQuestion();
                    return QuizResponseDTO.QuizQuestionResponseDTO.builder()
                            .quizQuestionId(qq.getQuizQuestionId())
                            .questionId(q.getQuestionId())
                            .questionContent(q.getContent())
                            .questionType(q.getQuestionType())
                            .skill(q.getSkill())
                            .cefrLevel(q.getCefrLevel())
                            .orderIndex(qq.getOrderIndex())
                            .points(qq.getPoints())
                            .build();
                })
                .collect(Collectors.toList());

        return QuizResponseDTO.builder()
                .quizId(quiz.getQuizId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .quizCategory(quiz.getQuizCategory())
                .courseId(quiz.getCourse() != null ? quiz.getCourse().getCourseId() : null)
                .courseName(quiz.getCourse() != null ? quiz.getCourse().getCourseName() : null)
                .status(quiz.getStatus())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passScore(quiz.getPassScore())
                .maxAttempts(quiz.getMaxAttempts())
                .numberOfQuestions(quiz.getNumberOfQuestions())
                .questionOrder(quiz.getQuestionOrder())
                .showAnswerAfterSubmit(quiz.getShowAnswerAfterSubmit())
                .createdByName(quiz.getUser() != null ? quiz.getUser().getFullName() : null)
                .createdAt(quiz.getCreatedAt())
                .updatedAt(quiz.getUpdatedAt())
                .totalQuestions(quizQuestions.size())
                .hasAttempts(hasAttempts)
                .questions(questionDTOs)
                .build();
    }
}
