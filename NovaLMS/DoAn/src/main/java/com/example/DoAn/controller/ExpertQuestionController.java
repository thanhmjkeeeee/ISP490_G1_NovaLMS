package com.example.DoAn.controller;

import com.example.DoAn.dto.request.AIGenerateRequestDTO;
import com.example.DoAn.dto.request.AIGenerateGroupRequestDTO;
import com.example.DoAn.dto.request.AIImportRequestDTO;
import com.example.DoAn.dto.request.AIImportGroupRequestDTO;
import com.example.DoAn.dto.request.ExcelImportGroupRequestDTO;
import com.example.DoAn.dto.request.ExcelImportRequestDTO;
import com.example.DoAn.dto.request.QuestionRequestDTO;
import com.example.DoAn.dto.request.QuestionGroupRequestDTO;
import com.example.DoAn.dto.response.AIGenerateResponseDTO;
import com.example.DoAn.dto.response.AIGenerateGroupResponseDTO;
import com.example.DoAn.dto.response.ExcelParseResultDTO;
import com.example.DoAn.dto.response.ExcelParseGroupResultDTO;
import com.example.DoAn.dto.response.QuestionResponseDTO;
import com.example.DoAn.dto.response.QuestionGroupResponseDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.AIQuestionService;
import com.example.DoAn.service.ExcelQuestionImportService;
import com.example.DoAn.service.IExpertQuestionService;
import com.example.DoAn.util.ExcelTemplateGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expert/questions")
@RequiredArgsConstructor
@Tag(name = "Expert - Question Management", description = "Quản lý câu hỏi và bộ câu hỏi bài đọc/nghe")
public class ExpertQuestionController {

    private final IExpertQuestionService questionService;
    private final AIQuestionService aiQuestionService;
    private final ExcelQuestionImportService excelService;
    private final ExcelTemplateGenerator templateGenerator;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    @Operation(summary = "Get all questions in a module")
    @GetMapping
    public ResponseData<List<QuestionResponseDTO>> getQuestions(
            @RequestParam Integer moduleId, Principal principal) {
        return ResponseData.success("Danh sách câu hỏi",
                questionService.getQuestionsByModule(moduleId, getEmail(principal)));
    }

    @Operation(summary = "Create a question with answer options")
    @PostMapping
    public ResponseData<QuestionResponseDTO> createQuestion(
            @Valid @RequestBody QuestionRequestDTO request, Principal principal) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Câu hỏi đã được tạo.",
                questionService.createQuestion(request, getEmail(principal)));
    }

    @Operation(summary = "Update a question and/or its answer options")
    @PutMapping("/{questionId}")
    public ResponseData<QuestionResponseDTO> updateQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody QuestionRequestDTO request, Principal principal) {
        return ResponseData.success("Câu hỏi đã được cập nhật.",
                questionService.updateQuestion(questionId, request, getEmail(principal)));
    }

    @Operation(summary = "Delete a question and all its answer options")
    @DeleteMapping("/{questionId}")
    public ResponseData<Void> deleteQuestion(
            @PathVariable Integer questionId, Principal principal) {
        questionService.deleteQuestion(questionId, getEmail(principal));
        return ResponseData.success("Câu hỏi đã được xóa.");
    }

    // ── Question Group Endpoints ─────────────────────────────────────────────

    @Operation(summary = "Create a question group (passage-based)")
    @PostMapping("/groups")
    public ResponseData<QuestionGroupResponseDTO> createQuestionGroup(
            @Valid @RequestBody QuestionGroupRequestDTO request, Principal principal) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Bộ câu hỏi đã được tạo.",
                questionService.createQuestionGroup(request, getEmail(principal)));
    }

    @Operation(summary = "Update a question group")
    @PutMapping("/groups/{groupId}")
    public ResponseData<QuestionGroupResponseDTO> updateQuestionGroup(
            @PathVariable Integer groupId,
            @Valid @RequestBody QuestionGroupRequestDTO request, Principal principal) {
        return ResponseData.success("Bộ câu hỏi đã được cập nhật.",
                questionService.updateQuestionGroup(groupId, request, getEmail(principal)));
    }

    @Operation(summary = "Delete a question group")
    @DeleteMapping("/groups/{groupId}")
    public ResponseData<Void> deleteQuestionGroup(
            @PathVariable Integer groupId, Principal principal) {
        questionService.deleteQuestionGroup(groupId, getEmail(principal));
        return ResponseData.success("Bộ câu hỏi đã được xóa.");
    }

    @Operation(summary = "Get my question groups")
    @GetMapping("/groups")
    public ResponseData<List<QuestionGroupResponseDTO>> getMyQuestionGroups(Principal principal) {
        return ResponseData.success("Danh sách bộ câu hỏi",
                questionService.getMyQuestionGroups(getEmail(principal)));
    }

    @Operation(summary = "Get a question group by ID")
    @GetMapping("/groups/{groupId}")
    public ResponseData<QuestionGroupResponseDTO> getQuestionGroupById(
            @PathVariable Integer groupId, Principal principal) {
        return ResponseData.success("Chi tiết bộ câu hỏi",
                questionService.getQuestionGroupById(groupId, getEmail(principal)));
    }

    // ── AI Generation ──────────────────────────────────────────────────────────

    @Operation(summary = "Generate questions using AI (preview only, not persisted)")
    @PostMapping("/ai/generate")
    public ResponseData<AIGenerateResponseDTO> generateQuestions(
            @Valid @RequestBody AIGenerateRequestDTO request, Principal principal) {
        if (!request.isValid()) {
            return ResponseData.error(400, "Phải nhập topic hoặc chọn module.");
        }
        AIGenerateResponseDTO result = aiQuestionService.generate(request, getEmail(principal));
        return ResponseData.success("Sinh câu hỏi thành công", result);
    }

    @Operation(summary = "Save selected AI-generated questions to DB")
    @PostMapping("/ai/import")
    public ResponseData<Void> importAIQuestions(
            @Valid @RequestBody AIImportRequestDTO request, Principal principal) {
        int saved = questionService.saveAIQuestions(request, getEmail(principal));
        return new ResponseData<>(HttpStatus.CREATED.value(),
                "Đã lưu " + saved + " câu hỏi.", null);
    }

    @Operation(summary = "Generate a passage-based question group using AI")
    @PostMapping("/ai/generate/group")
    public ResponseData<AIGenerateGroupResponseDTO> generateQuestionGroup(
            @Valid @RequestBody AIGenerateGroupRequestDTO request, Principal principal) {
        if (!request.isValid()) {
            return ResponseData.error(400, "Phải nhập topic hoặc chọn module.");
        }
        AIGenerateGroupResponseDTO result = aiQuestionService.generateGroup(request, getEmail(principal));
        return ResponseData.success("Sinh bộ câu hỏi thành công", result);
    }

    @Operation(summary = "Save selected AI-generated question group to DB")
    @PostMapping("/ai/import/group")
    public ResponseData<Void> importAIQuestionGroup(
            @Valid @RequestBody AIImportGroupRequestDTO request, Principal principal) {
        int saved = questionService.saveAIQuestionGroup(request, getEmail(principal));
        return new ResponseData<>(HttpStatus.CREATED.value(),
                "Đã lưu bộ câu hỏi với " + saved + " câu con.", null);
    }

    // ── Excel Import ───────────────────────────────────────────────────────────

    @Operation(summary = "Download Excel template for a question type")
    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> downloadTemplate(@RequestParam String type) {
        try {
            byte[] file = templateGenerator.generate(type);
            String filename = "template-" + type.toLowerCase() + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            return new ResponseEntity<>(file, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // template type invalid
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Parse uploaded Excel file and validate rows")
    @PostMapping(value = "/excel/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseData<ExcelParseResultDTO> parseExcel(
            @RequestParam String type,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseData.error(400, "Vui lòng chọn file Excel.");
            }
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return ResponseData.error(400, "Chỉ chấp nhận file .xlsx");
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseData.error(413, "File không được vượt quá 5MB.");
            }
            ExcelParseResultDTO result = excelService.parseFile(file, type);
            return ResponseData.success("Đã phân tích file", result);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi khi đọc file: " + e.getMessage());
        }
    }

    @Operation(summary = "Import validated questions from Excel")
    @PostMapping("/excel/import")
    public ResponseData<Void> importExcel(
            @Valid @RequestBody ExcelImportRequestDTO request, Principal principal) {
        try {
            int saved = excelService.importQuestions(request, getEmail(principal));
            return new ResponseData<>(HttpStatus.CREATED.value(),
                    "Đã import " + saved + " câu hỏi.", null);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi khi lưu: " + e.getMessage());
        }
    }

    @Operation(summary = "Parse uploaded Excel file for Question Group")
    @PostMapping(value = "/excel/parse-group", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseData<ExcelParseGroupResultDTO> parseGroupExcel(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseData.error(400, "Vui lòng chọn file Excel.");
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return ResponseData.error(400, "Chỉ chấp nhận file .xlsx");
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseData.error(413, "File không được vượt quá 5MB.");
            }
            ExcelParseGroupResultDTO result = excelService.parseGroupFile(file);
            return ResponseData.success("Đã phân tích file", result);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi khi đọc file: " + e.getMessage());
        }
    }

    @Operation(summary = "Import validated Question Groups from Excel")
    @PostMapping("/excel/import-group")
    public ResponseData<Void> importGroupExcel(
            @Valid @RequestBody ExcelImportGroupRequestDTO request, Principal principal) {
        try {
            int saved = excelService.importQuestionGroups(request, getEmail(principal));
            return new ResponseData<>(HttpStatus.CREATED.value(),
                    "Đã import bộ câu hỏi với " + saved + " câu con.", null);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi khi lưu: " + e.getMessage());
        }
    }
}
