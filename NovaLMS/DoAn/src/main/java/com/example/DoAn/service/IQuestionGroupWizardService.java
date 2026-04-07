package com.example.DoAn.service;

import com.example.DoAn.dto.request.*;
import com.example.DoAn.dto.response.*;
import jakarta.servlet.http.HttpSession;

import java.util.List;

public interface IQuestionGroupWizardService {

    void saveStep1(HttpSession session, WizardStep1DTO dto);

    WizardStep1DTO getStep1(HttpSession session);

    WizardStep2ResultDTO processStep2(HttpSession session, WizardStep2DTO dto, String email);

    List<ValidatedQuestionDTO> getQuestions(HttpSession session);

    WizardValidationResultDTO validate(HttpSession session);

    WizardValidationResultDTO getValidationResult(HttpSession session);

    Integer saveWizard(HttpSession session, WizardSaveDTO dto, String email);

    void abandon(HttpSession session);
}
