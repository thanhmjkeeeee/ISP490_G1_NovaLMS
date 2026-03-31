package com.example.DoAn.controller;

import com.example.DoAn.dto.request.HybridSessionCreateDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.service.FileUploadService;
import com.example.DoAn.service.HybridPlacementService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HybridPlacementController {

    private final HybridPlacementService hybridPlacementService;
    private final FileUploadService fileUploadService;

    // ══════════════════════════════════════════════════════════════
    // PAGE RENDERING
    // ══════════════════════════════════════════════════════════════

    /** Trang chọn kỹ năng — /hybrid-entry */
    @GetMapping("/hybrid-entry")
    public String showHybridEntry(Model model) {
        try {
            List<HybridSkillDTO> skills = hybridPlacementService.getAvailableSkills();
            model.addAttribute("skills", skills);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "public/hybrid-entry";
    }

    /** Trang chọn quiz mỗi kỹ năng — /hybrid-entry/select-quizzes?s=Grammar&s=Vocabulary */
    @GetMapping("/hybrid-entry/select-quizzes")
    public String showSelectQuizzes(
            @RequestParam List<String> s,
            Model model) {
        try {
            Map<String, List<HybridQuizSummaryDTO>> quizzes = hybridPlacementService.getQuizzesBySkills(s);
            model.addAttribute("selectedSkills", s);
            model.addAttribute("quizzesBySkill", quizzes);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hybrid-entry";
        }
        return "public/hybrid-select-quizzes";
    }

    /** Trang làm bài hybrid — /hybrid/{sessionId}/quiz/{quizIndex} */
    @GetMapping("/hybrid/{sessionId}/quiz/{quizIndex}")
    public String takeHybridQuiz(
            @PathVariable Integer sessionId,
            @PathVariable Integer quizIndex,
            Model model) {
        try {
            QuizTakingDTO quiz = hybridPlacementService.getQuizForSession(sessionId, quizIndex);
            model.addAttribute("quiz", quiz);
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("quizIndex", quizIndex);
            return "public/hybrid-quiz";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hybrid-entry";
        }
    }

    /** Màn hình chuyển phần — /hybrid/{sessionId}/transition */
    @GetMapping("/hybrid/{sessionId}/transition")
    public String showTransition(
            @PathVariable Integer sessionId,
            Model model) {
        try {
            HybridTransitionDTO info = hybridPlacementService.getTransitionInfo(sessionId);
            model.addAttribute("transition", info);
            return "public/hybrid-transition";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hybrid-entry";
        }
    }

    /** Trang kết quả tổng hợp — /hybrid/{sessionId}/results */
    @GetMapping("/hybrid/{sessionId}/results")
    public String showHybridResults(
            @PathVariable Integer sessionId,
            Model model) {
        try {
            HybridResultDTO result = hybridPlacementService.getHybridResults(sessionId);
            model.addAttribute("result", result);
            return "public/hybrid-results";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hybrid-entry";
        }
    }

    // ══════════════════════════════════════════════════════════════
    // API ENDPOINTS
    // ══════════════════════════════════════════════════════════════

    /** GET /api/v1/public/hybrid/skills — danh sách kỹ năng khả dụng */
    @GetMapping("/api/v1/public/hybrid/skills")
    @ResponseBody
    public ResponseEntity<?> getAvailableSkills() {
        try {
            List<HybridSkillDTO> skills = hybridPlacementService.getAvailableSkills();
            return ResponseEntity.ok(Map.of("status", 200, "data", skills));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        }
    }

    /** GET /api/v1/public/hybrid/quizzes?s=Grammar&s=Vocabulary */
    @GetMapping("/api/v1/public/hybrid/quizzes")
    @ResponseBody
    public ResponseEntity<?> getQuizzesBySkills(@RequestParam List<String> s) {
        try {
            Map<String, List<HybridQuizSummaryDTO>> quizzes = hybridPlacementService.getQuizzesBySkills(s);
            return ResponseEntity.ok(Map.of("status", 200, "data", quizzes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        }
    }

    /** POST /api/v1/public/hybrid/sessions — tạo phiên hybrid */
    @PostMapping("/api/v1/public/hybrid/sessions")
    @ResponseBody
    public ResponseEntity<?> createSession(
            @Valid @RequestBody HybridSessionCreateDTO request,
            HttpSession session) {
        try {
            HybridSessionDTO result = hybridPlacementService.createSession(request, session.getId());
            return ResponseEntity.ok(Map.of("status", 201, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        }
    }

    /** GET /api/v1/public/hybrid/sessions/{sessionId}/transition */
    @GetMapping("/api/v1/public/hybrid/sessions/{sessionId}/transition")
    @ResponseBody
    public ResponseEntity<?> getTransition(@PathVariable Integer sessionId) {
        try {
            HybridTransitionDTO info = hybridPlacementService.getTransitionInfo(sessionId);
            return ResponseEntity.ok(Map.of("status", 200, "data", info));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        }
    }

    /** GET /api/v1/public/hybrid/sessions/{sessionId}/results */
    @GetMapping("/api/v1/public/hybrid/sessions/{sessionId}/results")
    @ResponseBody
    public ResponseEntity<?> getHybridResults(@PathVariable Integer sessionId) {
        try {
            HybridResultDTO result = hybridPlacementService.getHybridResults(sessionId);
            return ResponseEntity.ok(Map.of("status", 200, "data", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        }
    }

    /** POST /api/v1/public/hybrid/audio — upload audio for SPEAKING questions */
    @PostMapping("/api/v1/public/hybrid/audio")
    @ResponseBody
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", 400, "message", "File is empty"));
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", 400, "message", "Only audio files allowed"));
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", 400, "message", "File too large (max 10MB)"));
            }
            String audioUrl = fileUploadService.upload(file);
            return ResponseEntity.ok(Map.of("status", 200, "audioUrl", audioUrl));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("status", 500, "message", "Upload failed: " + e.getMessage()));
        }
    }
}
