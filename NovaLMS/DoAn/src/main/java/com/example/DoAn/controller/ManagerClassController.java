package com.example.DoAn.controller;

import com.example.DoAn.dto.request.ClassRequestDTO;
import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.Registration;
import com.example.DoAn.repository.RegistrationRepository;
import com.example.DoAn.service.IClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
@Validated
@Slf4j
@Tag(name = "Class API Controller")
@RequiredArgsConstructor
public class ManagerClassController {

    private final IClassService classService;
    private final RegistrationRepository registrationRepository;

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

    @Operation(summary = "Get list of classes with pagination and separate filters")
    @GetMapping("/list")
    public ResponseData<PageResponse<ClassDetailResponse>> getList(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String status) {
        try {
            return new ResponseData<>(HttpStatus.OK.value(), "Success",
                    classService.getAllClasses(pageNo, pageSize, className, courseName, teacherName, status));
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Get class by ID")
    @GetMapping("/{id}")
    public ResponseData<ClassDetailResponse> getClassById(@PathVariable Integer id) {
        try {
            ClassDetailResponse response = classService.getClassById(id);
            return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Get class detail by ID")
    @GetMapping("/detail/{id}")
    public ResponseData<ClassDetailResponse> getClassDetailById(@PathVariable Integer id) {
        try {
            ClassDetailResponse response = classService.getClassById(id);
            return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Get available slot times by teacher and schedule")
    @GetMapping("/available-slots")
    public ResponseData<java.util.List<String>> getAvailableSlots(
            @RequestParam Integer teacherId,
            @RequestParam String schedule,
            @RequestParam(required = false) Integer excludeClassId) {
        try {
            java.util.List<String> slots = classService.getAvailableSlotTimes(teacherId, schedule, excludeClassId);
            return new ResponseData<>(HttpStatus.OK.value(), "Success", slots);
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Get available teachers by schedule and slot")
    @GetMapping("/available-teachers")
    public ResponseData<List<AccountDetailResponse>> getAvailableTeachers(
            @RequestParam String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam String schedule,
            @RequestParam String slotTime,
            @RequestParam(required = false) Integer excludeClassId) {
        try {
            List<com.example.DoAn.model.User> teachers = classService.getAvailableTeachers(startDate, endDate, schedule, slotTime, excludeClassId);
            List<AccountDetailResponse> response = teachers.stream()
                    .map(u -> AccountDetailResponse.builder()
                            .userId(u.getUserId())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .build())
                    .toList();
            return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }

    @Operation(summary = "Get students of a class with filters")
    @GetMapping("/{id}/students")
    public ResponseData<PageResponse<RegistrationResponseDTO>> getClassStudents(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        try {
            Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("registrationTime").descending());
            Page<Registration> page = registrationRepository.findByClassIdWithFilters(id, keyword, status, pageable);

            List<RegistrationResponseDTO> items = page.getContent().stream()
                    .map(r -> RegistrationResponseDTO.builder()
                            .registrationId(r.getRegistrationId())
                            .userId(r.getUser() != null ? r.getUser().getUserId() : null)
                            .userName(r.getUser() != null ? r.getUser().getFullName() : null)
                            .userEmail(r.getUser() != null ? r.getUser().getEmail() : null)
                            .classId(r.getClazz() != null ? r.getClazz().getClassId() : null)
                            .className(r.getClazz() != null ? r.getClazz().getClassName() : null)
                            .courseId(r.getCourse() != null ? r.getCourse().getCourseId() : null)
                            .courseName(r.getCourse() != null ? r.getCourse().getCourseName() : null)
                            .registrationTime(r.getRegistrationTime())
                            .status(r.getStatus())
                            .registrationPrice(r.getRegistrationPrice())
                            .note(r.getNote())
                            .build())
                    .toList();

            PageResponse<RegistrationResponseDTO> response = PageResponse.<RegistrationResponseDTO>builder()
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .totalPages(page.getTotalPages())
                    .totalElements((int) page.getTotalElements())
                    .last(pageNo >= page.getTotalPages() - 1)
                    .items(items)
                    .build();

            return new ResponseData<>(HttpStatus.OK.value(), "Success", response);
        } catch (Exception e) {
            return new ResponseError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        }
    }
}