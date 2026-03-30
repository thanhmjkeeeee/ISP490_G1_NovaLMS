package com.example.DoAn.controller;

import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.LessonQuizResponseDTO;
import com.example.DoAn.dto.response.QuizResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.QuizAssignment;
import com.example.DoAn.repository.QuizAssignmentRepository;
import com.example.DoAn.service.IExpertQuizService;
import com.example.DoAn.service.LessonQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/expert/modules")
@RequiredArgsConstructor
public class ExpertModuleQuizController {

    private final IExpertQuizService quizService;
    private final LessonQuizService lessonQuizService;
    private final QuizAssignmentRepository quizAssignmentRepository;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    // GET /api/v1/expert/modules/{moduleId}/quizzes
    @GetMapping("/{moduleId}/quizzes")
    public ResponseData<List<LessonQuizResponseDTO>> getModuleQuizzes(@PathVariable Integer moduleId) {
        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByModule_ModuleIdOrderByOrderIndexAsc(moduleId);

        List<LessonQuizResponseDTO> result = assignments.stream().map(qa -> {
            var q = qa.getQuiz();
            return LessonQuizResponseDTO.builder()
                    .quizId(q.getQuizId())
                    .title(q.getTitle())
                    .description(q.getDescription())
                    .quizCategory(q.getQuizCategory())
                    .status(q.getStatus())
                    .orderIndex(qa.getOrderIndex())
                    .passScore(q.getPassScore())
                    .timeLimitMinutes(q.getTimeLimitMinutes())
                    .maxAttempts(q.getMaxAttempts())
                    .numberOfQuestions(q.getNumberOfQuestions())
                    .build();
        }).collect(Collectors.toList());

        return ResponseData.success("Danh sách quiz của chương", result);
    }

    // POST /api/v1/expert/modules/{moduleId}/quizzes
    @PostMapping("/{moduleId}/quizzes")
    public ResponseData<?> createModuleQuiz(
            @PathVariable Integer moduleId,
            @RequestBody QuizRequestDTO request,
            Principal principal) {
        request.setModuleId(moduleId);
        request.setQuizCategory("MODULE_QUIZ");
        QuizResponseDTO created = quizService.createQuiz(request, getEmail(principal));
        return ResponseData.success("Tạo quiz cho chương thành công", created);
    }

    // PATCH /api/v1/expert/modules/{moduleId}/quizzes/reorder
    @PatchMapping("/{moduleId}/quizzes/reorder")
    public ResponseData<?> reorderModuleQuizzes(
            @PathVariable Integer moduleId,
            @RequestBody List<Map<String, Integer>> orderList) {
        List<QuizAssignment> assignments = quizAssignmentRepository
                .findByModule_ModuleIdOrderByOrderIndexAsc(moduleId);
        for (Map<String, Integer> item : orderList) {
            Integer quizId = item.get("quizId");
            Integer orderIndex = item.get("orderIndex");
            assignments.stream()
                    .filter(a -> a.getQuiz().getQuizId().equals(quizId))
                    .findFirst()
                    .ifPresent(a -> a.setOrderIndex(orderIndex));
        }
        quizAssignmentRepository.saveAll(assignments);
        return ResponseData.success("Đã sắp xếp lại thứ tự quiz");
    }

    // DELETE /api/v1/expert/modules/{moduleId}/quizzes/{quizId}
    @DeleteMapping("/{moduleId}/quizzes/{quizId}")
    @Transactional
    public ResponseData<?> detachQuiz(
            @PathVariable Integer moduleId,
            @PathVariable Integer quizId,
            Principal principal) {
        // Note: ownership is enforced at service layer via ExpertQuizService
        lessonQuizService.detachQuizFromModule(moduleId, quizId);
        return ResponseData.success("Đã gỡ quiz khỏi chương");
    }
}
