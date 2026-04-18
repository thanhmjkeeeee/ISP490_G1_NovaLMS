package com.example.DoAn.controller;

import com.example.DoAn.dto.request.AIGenerateRequestDTO;
import com.example.DoAn.dto.request.AIGenerateGroupRequestDTO;
import com.example.DoAn.dto.request.AIImportRequestDTO;
import com.example.DoAn.dto.request.QuestionBankRequestDTO;
import com.example.DoAn.dto.request.QuizRequestDTO;
import com.example.DoAn.dto.response.QuizSkillSummaryDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.AIQuestionService;
import com.example.DoAn.service.TeacherQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/quizzes")
@RequiredArgsConstructor
public class TeacherQuizApiController {

    private final TeacherQuizService teacherQuizService;
    private final AIQuestionService aiQuestionService;
    private final com.example.DoAn.service.QuizResultService quizResultService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getPrincipal().getAttribute("email");
        }
        return principal != null ? principal.getName() : null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  QUIZ CRUD
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Tạo quiz mới cho class.
     * POST /api/v1/teacher/quizzes
     * Body: { title, description, classId, timeLimitMinutes, passScore, maxAttempts, numberOfQuestions, questionOrder, showAnswerAfterSubmit }
     */
    @PostMapping
    public ResponseData<?> createQuiz(@RequestBody QuizRequestDTO request, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.createQuiz(request, email);
    }

    /**
     * Lấy chi tiết quiz.
     * GET /api/v1/teacher/quizzes/{quizId}
     */
    @GetMapping("/{quizId}")
    public ResponseData<?> getQuiz(@PathVariable Integer quizId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.getQuizById(quizId);
    }

    /**
     * Danh sách quiz của một class.
     * GET /api/v1/teacher/quizzes/class/{classId}
     */
    @GetMapping("/class/{classId}")
    public ResponseData<List<TeacherQuizService.TeacherQuizDTO>> getQuizzesByClass(@PathVariable Integer classId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.getQuizzesByClass(classId, email);
    }

    /**
     * Cập nhật quiz.
     * PUT /api/v1/teacher/quizzes/{quizId}
     */
    @PutMapping("/{quizId}")
    public ResponseData<?> updateQuiz(@PathVariable Integer quizId, @RequestBody QuizRequestDTO request, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.updateQuiz(quizId, request, email);
    }

    /**
     * Publish quiz (chuyển từ DRAFT -> PUBLISHED để sinh viên có thể làm).
     * POST /api/v1/teacher/quizzes/{quizId}/publish
     */
    @PostMapping("/{quizId}/publish")
    public ResponseData<?> publishQuiz(@PathVariable Integer quizId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.publishQuiz(quizId, email);
    }

    /**
     * Mở/đóng quiz cho học sinh làm (toggle isOpen).
     * PATCH /api/v1/teacher/quizzes/{quizId}/toggle-open
     */
    @PatchMapping("/{quizId}/toggle-open")
    public ResponseData<?> toggleQuizOpen(@PathVariable Integer quizId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.toggleQuizOpen(quizId, email);
    }

    /**
     * Xóa quiz.
     * DELETE /api/v1/teacher/quizzes/{quizId}
     */
    @DeleteMapping("/{quizId}")
    public ResponseData<?> deleteQuiz(@PathVariable Integer quizId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.deleteQuiz(quizId, email);
    }

    /**
     * Mở khóa bài quiz bị khóa do student vi phạm.
     * POST /api/v1/teacher/quizzes/results/{resultId}/unlock
     */
    @PostMapping("/results/{resultId}/unlock")
    public ResponseData<?> unlockQuizResult(@PathVariable Integer resultId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        try {
            quizResultService.unlockQuiz(resultId);
            return ResponseData.success("Mở khóa bài làm thành công.");
        } catch (Exception e) {
            return ResponseData.error(400, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  QUESTIONS FROM MASTER BANK
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách câu hỏi từ Master Bank (chỉ PUBLISHED).
     * GET /api/v1/teacher/quizzes/bank-questions?skill=READING&cefrLevel=B1&questionType=MULTIPLE_CHOICE_SINGLE&keyword=...
     */
    @GetMapping("/bank-questions")
    public ResponseData<List<TeacherQuizService.QuestionBankSimpleDTO>> getBankQuestions(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String cefrLevel,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String keyword,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.getPublishedQuestions(skill, cefrLevel, questionType, keyword);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CREATE NEW PRIVATE QUESTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Tạo câu hỏi mới riêng cho quiz (không lưu vào bank).
     * POST /api/v1/teacher/quizzes/questions
     * Body: QuestionBankRequestDTO (content, questionType, skill, cefrLevel, topic, tags, explanation, options...)
     * Trả về: TeacherQuestionDTO với status=PENDING_REVIEW
     */
    @PostMapping("/questions")
    public ResponseData<?> createPrivateQuestion(@RequestBody QuestionBankRequestDTO request, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.createQuestion(request, email);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MANAGE QUESTIONS IN QUIZ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Thêm câu hỏi (từ bank hoặc private) vào quiz.
     * POST /api/v1/teacher/quizzes/{quizId}/questions
     * Body: { questionId, orderIndex, points }
     */
    @PostMapping("/{quizId}/questions")
    public ResponseData<?> addQuestionToQuiz(
            @PathVariable Integer quizId,
            @RequestBody QuestionBankRequestDTO request,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.addQuestionToQuiz(
                quizId, request.getQuestionIdForQuiz(),
                request.getOrderIndex(), request.getPoints(), email);
    }

    /**
     * Xóa câu hỏi khỏi quiz.
     * DELETE /api/v1/teacher/quizzes/{quizId}/questions/{questionId}
     */
    @DeleteMapping("/{quizId}/questions/{questionId}")
    public ResponseData<?> removeQuestionFromQuiz(
            @PathVariable Integer quizId,
            @PathVariable Integer questionId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.removeQuestionFromQuiz(quizId, questionId, email);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SKILL SUMMARY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Lấy skill summary: question count per skill, broken down by PUBLISHED vs PENDING_REVIEW.
     * GET /api/v1/teacher/quizzes/{quizId}/skill-summary
     */
    @GetMapping("/{quizId}/skill-summary")
    public ResponseData<List<QuizSkillSummaryDTO>> getSkillSummary(
            @PathVariable Integer quizId,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return ResponseData.success(teacherQuizService.getSkillSummary(quizId));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AI QUESTION GENERATION (Teacher)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Sinh câu hỏi bằng AI (teacher).
     * POST /api/v1/teacher/quizzes/ai/generate
     * Body: { topic, quantity, skill?, cefrLevel?, questionTypes? }
     */
    @PostMapping("/ai/generate")
    public ResponseData<?> generateAIQuestions(
            @Valid @RequestBody AIGenerateRequestDTO request,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return ResponseData.success("Sinh câu hỏi thành công", aiQuestionService.generate(request, email));
    }

    /**
     * Sinh bộ câu hỏi (group) bằng AI (teacher).
     * POST /api/v1/teacher/quizzes/ai/generate-group
     */
    @PostMapping("/ai/generate-group")
    public ResponseData<?> generateAIGroupQuestions(
            @Valid @RequestBody  AIGenerateGroupRequestDTO request,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return ResponseData.success("Sinh bộ câu hỏi thành công", aiQuestionService.generateGroup(request, email));
    }

    /**
     * Import câu hỏi AI đã chọn vào quiz.
     * POST /api/v1/teacher/quizzes/ai/import
     * Body: { quizId, questions: [...] }
     */
    @PostMapping("/ai/import")
    public ResponseData<?> importAIQuestions(
            @RequestBody TeacherQuizService.AIImportRequestDTO request,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.importAIQuestions(request, email);
    }

    /**
     * Gửi câu hỏi lên expert để duyệt.
     * POST /api/v1/teacher/quizzes/questions/{questionId}/submit
     */
    @PostMapping("/questions/{questionId}/submit")
    public ResponseData<?> submitQuestionForReview(@PathVariable Integer questionId, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.submitQuestionForReview(questionId, email);
    }

    /**
     * Gửi đồng loạt nhiều câu hỏi lên expert.
     * POST /api/v1/teacher/quizzes/questions/submit-batch
     */
    @PostMapping("/questions/submit-batch")
    public ResponseData<?> submitQuestionsBatch(@RequestBody List<Integer> questionIds, Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseData.error(401, "Unauthorized");
        return teacherQuizService.submitQuestionsBatch(questionIds, email);
    }
}
