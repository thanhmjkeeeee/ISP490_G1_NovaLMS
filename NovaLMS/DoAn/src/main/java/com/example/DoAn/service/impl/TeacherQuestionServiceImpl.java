package com.example.DoAn.service.impl;

import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.Question;
import com.example.DoAn.model.User;
import com.example.DoAn.model.AnswerOption;
import com.example.DoAn.repository.QuestionRepository;
import com.example.DoAn.repository.AnswerOptionRepository;
import com.example.DoAn.service.ITeacherQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherQuestionServiceImpl implements ITeacherQuestionService {

    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;

    @Override
    @Transactional
    public void cancelSubmitQuestion(Integer questionId, String teacherEmail) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + questionId));

        // Check ownership (security)
        if (question.getUser() == null || !question.getUser().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Bạn không có quyền xử lý câu hỏi này.");
        }

        // Check status
        if (!"PENDING_REVIEW".equals(question.getStatus())) {
            throw new RuntimeException("Chỉ có thể hủy câu hỏi đang chờ duyệt (PENDING_REVIEW).");
        }

        // Update status to DRAFT
        question.setStatus("DRAFT");
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public void submitQuestion(Integer questionId, String teacherEmail) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + questionId));

        // Check ownership (security)
        if (question.getUser() == null || !question.getUser().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Bạn không có quyền xử lý câu hỏi này.");
        }

        // Check status
        if (!"DRAFT".equals(question.getStatus()) && !"REJECTED".equals(question.getStatus())) {
            throw new RuntimeException("Chỉ có thể gửi phê duyệt câu hỏi ở trạng thái DRAFT hoặc REJECTED.");
        }

        // Update status to PENDING_REVIEW
        question.setStatus("PENDING_REVIEW");
        questionRepository.save(question);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void updateQuestion(Integer questionId, java.util.Map<String, Object> data, String teacherEmail) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi với ID: " + questionId));

        if (question.getUser() == null || !question.getUser().getEmail().equals(teacherEmail)) {
            throw new RuntimeException("Bạn không có quyền sửa câu hỏi này.");
        }

        if (!"DRAFT".equals(question.getStatus()) && !"REJECTED".equals(question.getStatus())) {
            throw new RuntimeException("Chỉ có thể sửa câu hỏi ở trạng thái DRAFT hoặc REJECTED.");
        }

        if (data.containsKey("content")) {
            question.setContent((String) data.get("content"));
        }
        if (data.containsKey("topic")) {
            question.setTopic((String) data.get("topic"));
        }

        // [FIX] An toàn hóa việc ép kiểu dữ liệu từ JSON Map
        if (data.containsKey("options")) {
            java.util.List<java.util.Map<String, Object>> optionsData =
                    (java.util.List<java.util.Map<String, Object>>) data.get("options");

            for (var optMap : optionsData) {
                if (optMap.get("optionId") == null) continue;

                // Ép kiểu an toàn (chống lỗi ClassCastException nếu JS gửi lên String)
                Integer optId = Integer.valueOf(optMap.get("optionId").toString());
                String optText = (String) optMap.get("optionText");
                Boolean isCorrect = (Boolean) optMap.get("isCorrect");

                AnswerOption opt = answerOptionRepository.findById(optId).orElse(null);

                // Đảm bảo đáp án tồn tại và thực sự thuộc về câu hỏi này
                if (opt != null && opt.getQuestion().getQuestionId().equals(questionId)) {
                    opt.setTitle(optText);
                    opt.setCorrectAnswer(isCorrect != null && isCorrect);
                    answerOptionRepository.save(opt);
                }
            }
        }

        questionRepository.save(question);
    }
}
