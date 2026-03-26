package com.example.DoAn.controller;

import com.example.DoAn.dto.request.PlacementTestSubmissionDTO;
import com.example.DoAn.dto.response.PlacementTestSummaryDTO;
import com.example.DoAn.dto.response.QuizTakingDTO;
import com.example.DoAn.service.PlacementTestService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PlacementTestControllerNN {

    private final PlacementTestService placementTestService;

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
}
