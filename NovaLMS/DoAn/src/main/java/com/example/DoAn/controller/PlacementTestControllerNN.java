package com.example.DoAn.controller;

import com.example.DoAn.dto.request.PlacementTestSubmissionDTO;
import com.example.DoAn.service.FileUploadService;
import com.example.DoAn.service.PlacementTestService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PlacementTestControllerNN {

    private final PlacementTestService placementTestService;
    private final FileUploadService fileUploadService;

    // ══════════════════════════════════════════════════════════════
    // LEGACY / STANDALONE — redirect to hybrid flow
    // ══════════════════════════════════════════════════════════════

    /** Old standalone entry test page → hybrid entry */
    @GetMapping("/placement-test")
    public String showPlacementTestList() {
        return "redirect:/hybrid-entry";
    }

    /** Old standalone quiz taking page → hybrid entry */
    @GetMapping("/placement-test/{quizId}")
    public String takePlacementTest(@PathVariable Integer quizId) {
        return "redirect:/hybrid-entry";
    }

    // ══════════════════════════════════════════════════════════════
    // HYBRID SHARED ENDPOINTS (used by hybrid-quiz.html)
    // ══════════════════════════════════════════════════════════════

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
