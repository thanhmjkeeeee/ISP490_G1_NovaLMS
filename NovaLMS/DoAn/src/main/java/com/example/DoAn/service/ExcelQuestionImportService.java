package com.example.DoAn.service;

import com.example.DoAn.dto.request.ExcelImportRequestDTO;
import com.example.DoAn.dto.response.ExcelParseResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelQuestionImportService {

    ExcelParseResultDTO parseFile(MultipartFile file, String questionType) throws Exception;

    int importQuestions(ExcelImportRequestDTO request, String userEmail) throws Exception;
}
