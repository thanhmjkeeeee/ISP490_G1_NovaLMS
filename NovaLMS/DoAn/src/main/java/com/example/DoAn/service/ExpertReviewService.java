package com.example.DoAn.service;

import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.Question;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertReviewService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách câu hỏi TEACHER_PRIVATE đang chờ duyệt.
     */
    @Transactional(readOnly = true)
    public ResponseData<List<PendingQuestionDTO>> getPendingQuestions() {
        try {
            List<Question> questions = questionRepository.findAll().stream()
                    .filter(q -> "PENDING_REVIEW".equals(q.getStatus())
                            && "TEACHER_PRIVATE".equals(q.getSource()))
                    .toList();

            List<PendingQuestionDTO> dtos = questions.stream().map(q ->
                    PendingQuestionDTO.builder()
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
                            .createdByName(q.getUser() != null ? q.getUser().getFullName() : null)
                            .createdByEmail(q.getUser() != null ? q.getUser().getEmail() : null)
                            .createdAt(q.getCreatedAt())
                            .build()
            ).toList();

            return ResponseData.success("Danh sách câu hỏi chờ duyệt", dtos);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Expert duyệt câu hỏi: PENDING_REVIEW -> PUBLISHED
     */
    @Transactional
    public ResponseData<PendingQuestionDTO> approveQuestion(Integer questionId, String email) {
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

            question.setStatus("PUBLISHED");
            // source giữ nguyên TEACHER_PRIVATE để biết nguồn gốc
            questionRepository.save(question);

            return ResponseData.success("Đã duyệt câu hỏi", toDTO(question));
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    /**
     * Expert từ chối: PENDING_REVIEW -> DRAFT (hoặc xóa)
     */
    @Transactional
    public ResponseData<Void> rejectQuestion(Integer questionId, String email, boolean deleteQuestion) {
        try {
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question == null) {
                return ResponseData.error(404, "Không tìm thấy câu hỏi");
            }

            if (!"PENDING_REVIEW".equals(question.getStatus())) {
                return ResponseData.error(400, "Câu hỏi không ở trạng thái chờ duyệt");
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
        private String createdByName;
        private String createdByEmail;
        private java.time.LocalDateTime createdAt;
    }
}
