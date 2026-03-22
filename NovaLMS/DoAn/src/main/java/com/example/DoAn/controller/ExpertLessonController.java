package com.example.DoAn.controller;

import com.example.DoAn.dto.request.LessonRequestDTO;
import com.example.DoAn.dto.response.ExpertLessonResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.IExpertLessonService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expert/lessons")
@RequiredArgsConstructor
public class ExpertLessonController {

    private final IExpertLessonService lessonService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    @Operation(summary = "Get all lessons for a module")
    @GetMapping
    public ResponseData<List<ExpertLessonResponseDTO>> getLessons(
            @RequestParam Integer moduleId, Principal principal) {
        return ResponseData.success("Danh sách bài học",
                lessonService.getLessonsByModule(moduleId, getEmail(principal)));
    }

    @Operation(summary = "Create a new lesson")
    @PostMapping
    public ResponseData<ExpertLessonResponseDTO> createLesson(
            @Valid @RequestBody LessonRequestDTO request, Principal principal) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Bài học đã được tạo.",
                lessonService.createLesson(request, getEmail(principal)));
    }

    @Operation(summary = "Update a lesson")
    @PutMapping("/{lessonId}")
    public ResponseData<ExpertLessonResponseDTO> updateLesson(
            @PathVariable Integer lessonId,
            @Valid @RequestBody LessonRequestDTO request, Principal principal) {
        return ResponseData.success("Bài học đã được cập nhật.",
                lessonService.updateLesson(lessonId, request, getEmail(principal)));
    }

    @Operation(summary = "Delete a lesson")
    @DeleteMapping("/{lessonId}")
    public ResponseData<Void> deleteLesson(
            @PathVariable Integer lessonId, Principal principal) {
        lessonService.deleteLesson(lessonId, getEmail(principal));
        return ResponseData.success("Bài học đã được xóa.");
    }
}
