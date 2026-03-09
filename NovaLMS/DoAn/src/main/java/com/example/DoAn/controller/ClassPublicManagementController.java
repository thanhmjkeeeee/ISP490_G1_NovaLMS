package com.example.DoAn.controller;

import com.example.DoAn.dto.response.ClassPublicResponseDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.ClassPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassPublicManagementController {

    private final ClassPublicService classPublicService;

    @GetMapping("/public-list")
    public ResponseData<PageResponse<ClassPublicResponseDTO>> getClasses(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer categoryId) {

        var response = classPublicService.getOpenClassesWithFilter(pageNo, pageSize, categoryId);
        return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
    }
}