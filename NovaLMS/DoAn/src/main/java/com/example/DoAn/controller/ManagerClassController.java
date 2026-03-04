package com.example.DoAn.controller;

import com.example.DoAn.dto.request.ClassRequestDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.service.IClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/classes")
@Validated
@Slf4j
@Tag(name = "Class API Controller")
@RequiredArgsConstructor
public class ManagerClassController {

    private final IClassService classService;

    @Operation(summary = "Add new class")
    @PostMapping("/")
    public ResponseData<Integer> addClass(@Valid @RequestBody ClassRequestDTO request) {
        try {
            Integer classId = classService.saveClass(request);
            return new ResponseData<>(HttpStatus.CREATED.value(), "Success", classId);
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Update class info")
    @PutMapping("/{id}")
    public ResponseData<Void> updateClass(@PathVariable Integer id, @Valid @RequestBody ClassRequestDTO request) {
        try {
            classService.updateClass(id, request);
            return new ResponseData<>(HttpStatus.ACCEPTED.value(), "Updated");
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Get list of classes with pagination")
    @GetMapping("/list")
    public ResponseData<PageResponse<?>> getList(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            return new ResponseData<>(HttpStatus.OK.value(), "Success", classService.getAllClasses(pageNo, pageSize));
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
}