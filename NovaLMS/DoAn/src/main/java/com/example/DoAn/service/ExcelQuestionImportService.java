package com.example.DoAn.service;

import com.example.DoAn.dto.request.ExcelImportGroupRequestDTO;
import com.example.DoAn.dto.request.ExcelImportRequestDTO;
import com.example.DoAn.dto.response.ExcelParseGroupResultDTO;
import com.example.DoAn.dto.response.ExcelParseResultDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelQuestionImportService {

    ExcelParseResultDTO parseFile(MultipartFile file, String questionType) throws Exception;

    int importQuestions(ExcelImportRequestDTO request, String userEmail) throws Exception;

    ExcelParseGroupResultDTO parseGroupFile(MultipartFile file) throws Exception;

    int importQuestionGroups(ExcelImportGroupRequestDTO request, String userEmail) throws Exception;
}
