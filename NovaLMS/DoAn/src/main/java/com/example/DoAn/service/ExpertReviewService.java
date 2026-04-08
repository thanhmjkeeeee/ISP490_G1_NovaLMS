package com.example.DoAn.service;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Question;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpertReviewService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final INotificationService notificationService;
    private final EmailService emailService;

    public ExpertReviewService(QuestionRepository questionRepository,
                              UserRepository userRepository,
                              INotificationService notificationService,
                              EmailService emailService) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    /**
     * Lấy danh sách câu hỏi TEACHER_PRIVATE đang chờ duyệt.
     */
    @Transactional(readOnly = true)
    public ResponseData<List<PendingQuestionDTO>> getPendingQuestions() {
        try {
            List<Question> questions = questionRepository.findAll().stream()
                    .filter(q -> "PENDING_REVIEW".equals(q.getStatus())
                            && "TEACHER_PRIVATE".equals(q.getSource()))
                    .collect(Collectors.toList());

            List<PendingQuestionDTO> dtos = questions.stream().map(this::toDTO).collect(Collectors.toList());
            return ResponseData.success("Danh sách câu hỏi chờ duyệt", dtos);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Lấy chi tiết một câu hỏi.
     */
    @Transactional(readOnly = true)
    public ResponseData<PendingQuestionDTO> getQuestionById(Integer questionId) {
        try {
            Question q = questionRepository.findById(questionId).orElse(null);
            if (q == null) return ResponseData.error(404, "Không tìm thấy câu hỏi");
            return ResponseData.success(toDTO(q));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Expert duyệt câu hỏi: PENDING_REVIEW -> PUBLISHED
     */
    @Transactional
    public ResponseData<PendingQuestionDTO> approveQuestion(Integer questionId, String email, String reviewNote) {
        try {
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null) {
                return ResponseData.error(404, "Không tìm thấy câu hỏi");
            }

            if (!"PENDING_REVIEW".equals(question.getStatus())) {
                return ResponseData.error(400, "Câu hỏi không ở trạng thái chờ duyệt");
            }
            if (!"TEACHER_PRIVATE".equals(question.getSource())) {
                return ResponseData.error(400, "Chỉ câu hỏi từ giáo viên mới cần duyệt");
            }

            User expert = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Expert not found"));

            question.setStatus("PUBLISHED");
            question.setReviewerId(Long.valueOf(expert.getUserId()));
            question.setReviewedAt(LocalDateTime.now());
            question.setReviewNote(reviewNote);
            questionRepository.save(question);

            // Fire notification to teacher
            if (question.getUser() != null) {
                User teacher = question.getUser();
                notificationService.sendQuestionApproved(
                        Long.valueOf(teacher.getUserId()),
                        question.getContent(),
                        null
                );

                // Also send email
                if (teacher.getEmail() != null && !teacher.getEmail().isBlank()) {
                    String teacherName = teacher.getFullName() != null ? teacher.getFullName() : "";
                    String content = question.getContent() != null && question.getContent().length() > 80
                            ? question.getContent().substring(0, 80) + "..." : question.getContent();
                    emailService.sendQuestionApprovedEmail(teacher.getEmail(), teacherName, content, null);
                }
            }

            return ResponseData.success("Đã duyệt câu hỏi", toDTO(question));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Expert từ chối: PENDING_REVIEW -> DRAFT (hoặc xóa)
     */
    @Transactional
    public ResponseData<Void> rejectQuestion(Integer questionId, String email, boolean deleteQuestion, String reviewNote) {
        try {
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null) {
                return ResponseData.error(404, "Không tìm thấy câu hỏi");
            }

            if (!"PENDING_REVIEW".equals(question.getStatus())) {
                return ResponseData.error(400, "Câu hỏi không ở trạng thái chờ duyệt");
            }

            User expert = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Expert not found"));

            question.setReviewerId(Long.valueOf(expert.getUserId()));
            question.setReviewedAt(LocalDateTime.now());
            question.setReviewNote(reviewNote);

            // Fire notification + email to teacher
            if (question.getUser() != null) {
                User teacher = question.getUser();
                String teacherName = teacher.getFullName() != null ? teacher.getFullName() : "";
                String content = question.getContent() != null && question.getContent().length() > 80
                        ? question.getContent().substring(0, 80) + "..." : question.getContent();

                notificationService.sendQuestionRejected(
                        Long.valueOf(teacher.getUserId()),
                        question.getContent(),
                        reviewNote
                );

                if (teacher.getEmail() != null && !teacher.getEmail().isBlank()) {
                    emailService.sendQuestionRejectedEmail(teacher.getEmail(), teacherName, content, reviewNote);
                }
            }

            if (deleteQuestion) {
                questionRepository.delete(question);
                return ResponseData.success("Đã xóa câu hỏi");
            } else {
                question.setStatus("DRAFT");
                questionRepository.save(question);
                return ResponseData.success("Đã trả lại câu hỏi về bản nháp");
            }
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    private PendingQuestionDTO toDTO(Question q) {
        return PendingQuestionDTO.builder()
                .questionId(q.getQuestionId())
                .content(q.getContent())
                .questionType(q.getQuestionType())
                .skill(q.getSkill())
                .cefrLevel(q.getCefrLevel())
                .topic(q.getTopic())
                .tags(q.getTags())
                .explanation(q.getExplanation())
                .audioUrl(q.getAudioUrl())
                .imageUrl(q.getImageUrl())
                .source(q.getSource())
                .status(q.getStatus())
                .reviewerId(q.getReviewerId())
                .reviewedAt(q.getReviewedAt())
                .reviewNote(q.getReviewNote())
                .createdByName(q.getUser() != null ? q.getUser().getFullName() : null)
                .createdByEmail(q.getUser() != null ? q.getUser().getEmail() : null)
                .createdAt(q.getCreatedAt())
                .build();
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class PendingQuestionDTO {
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
        private String source;
        private String status;
        private Long reviewerId;
        private java.time.LocalDateTime reviewedAt;
        private String reviewNote;
        private String createdByName;
        private String createdByEmail;
        private java.time.LocalDateTime createdAt;
    }
}
