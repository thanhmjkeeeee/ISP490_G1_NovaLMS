package com.example.DoAn.controller;

import com.example.DoAn.dto.request.*;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.service.IQuestionGroupWizardService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expert/questions/wizard")
@RequiredArgsConstructor
public class ExpertQuestionGroupWizardApiController {

    private final IQuestionGroupWizardService wizardService;

    @PostMapping("/step1")
    public ResponseEntity<ResponseData<Void>> submitStep1(
            @Valid @RequestBody WizardStep1DTO dto,
            HttpSession session) {
        wizardService.saveStep1(session, dto);
        return ResponseEntity.ok(ResponseData.success("Đã lưu bước 1", null));
    }

    @PostMapping("/step2")
    public ResponseEntity<ResponseData<WizardStep2ResultDTO>> submitStep2(
            @Valid @ModelAttribute WizardStep2DTO dto,
            HttpSession session,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseEntity.status(401)
                .body(ResponseData.error(401, "Vui lòng đăng nhập."));
        WizardStep2ResultDTO result = wizardService.processStep2(session, dto, email);
        return ResponseEntity.ok(ResponseData.success("Đã xử lý bước 2", result));
    }

    @PostMapping("/validate")
    public ResponseEntity<ResponseData<WizardValidationResultDTO>> validate(HttpSession session) {
        WizardValidationResultDTO result = wizardService.validate(session);
        return ResponseEntity.ok(ResponseData.success("Đã kiểm tra dữ liệu", result));
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseData<Integer>> save(
            @Valid @RequestBody WizardSaveDTO dto,
            HttpSession session,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseEntity.status(401)
                .body(ResponseData.error(401, "Vui lòng đăng nhập."));
        Integer groupId = wizardService.saveWizard(session, dto, email);
        return ResponseEntity.ok(ResponseData.success("Đã lưu nhóm câu hỏi", groupId));
    }

    @GetMapping("/step-data")
    public ResponseEntity<ResponseData<WizardStepDataDTO>> getStepData(HttpSession session) {
        WizardStep1DTO step1 = wizardService.getStep1(session);
        List<ValidatedQuestionDTO> questions = wizardService.getQuestions(session);
        WizardValidationResultDTO validation = wizardService.getValidationResult(session);
        WizardStepDataDTO data = WizardStepDataDTO.builder()
                .step1(step1)
                .questions(questions)
                .validationResult(validation)
                .build();
        return ResponseEntity.ok(ResponseData.success("Đã tải dữ liệu wizard", data));
    }

    @PostMapping("/step2/manual")
    public ResponseEntity<ResponseData<WizardStep2ResultDTO>> submitManualQuestions(
            @RequestBody WizardStep2DTO dto,
            HttpSession session,
            Principal principal) {
        String email = getEmail(principal);
        if (email == null) return ResponseEntity.status(401)
                .body(ResponseData.error(401, "Vui lòng đăng nhập."));
        WizardStep2ResultDTO result = wizardService.processStep2(session, dto, email);
        return ResponseEntity.ok(ResponseData.success("Đã thêm câu hỏi thủ công", result));
    }

    @GetMapping("/abandon")
    public ResponseEntity<ResponseData<Void>> abandon(HttpSession session) {
        wizardService.abandon(session);
        return ResponseEntity.ok(ResponseData.success("Đã hủy wizard", null));
    }

    private String getEmail(Principal principal) {
        if (principal == null) return null;
        return principal.getName();
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class WizardStepDataDTO {
        private WizardStep1DTO step1;
        private List<ValidatedQuestionDTO> questions;
        private WizardValidationResultDTO validationResult;
    }
}
