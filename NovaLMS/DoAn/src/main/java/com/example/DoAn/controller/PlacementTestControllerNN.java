package com.example.DoAn.controller;

import com.example.DoAn.dto.request.PlacementTestSubmissionDTO;
import com.example.DoAn.dto.response.PlacementTestSummaryDTO;
import com.example.DoAn.dto.response.QuizTakingDTO;
import com.example.DoAn.service.FileUploadService;
import com.example.DoAn.service.PlacementTestService;
import jakarta.servlet.http.HttpSession;
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
public class PlacementTestControllerNN {

    private final PlacementTestService placementTestService;
    private final FileUploadService fileUploadService;

    @GetMapping("/placement-test")
    public String showPlacementTestList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String skill,
            Model model) {
        try {
            List<PlacementTestSummaryDTO> tests = placementTestService.getPlacementTests(keyword, skill);
            model.addAttribute("tests", tests);
            return "public/placement-test";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "public/placement-test";
        }
    }

    @GetMapping("/api/v1/public/placement-tests")
    @ResponseBody
    public ResponseEntity<?> getPlacementTestsApi(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String skill) {
        try {
            List<PlacementTestSummaryDTO> tests = placementTestService.getPlacementTests(keyword, skill);
            return ResponseEntity.ok(Map.of("status", 200, "data", tests));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", 400, "message", e.getMessage()));
        }
    }

    @GetMapping("/placement-test/{quizId}")
    public String takePlacementTest(@PathVariable Integer quizId, Model model) {
        try {
            QuizTakingDTO quiz = placementTestService.getPlacementTest(quizId);
            model.addAttribute("quiz", quiz);
            return "public/placement-test";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            List<PlacementTestSummaryDTO> tests = placementTestService.getAllPlacementTests();
            model.addAttribute("tests", tests);
            return "public/placement-test";
        }
    }

    @PostMapping("/api/v1/public/placement-test/submit")
    @ResponseBody
    public ResponseEntity<?> submitPlacementTest(
            @RequestBody PlacementTestSubmissionDTO request,
            HttpSession session) {
        try {
            String sessionId = session.getId();
            Integer resultId = placementTestService.submitPlacementTest(request, sessionId);
            return ResponseEntity.ok(Map.of("resultId", resultId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/v1/public/placement-test/audio")
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
