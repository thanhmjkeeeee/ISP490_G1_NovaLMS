package com.example.DoAn.controller;

import com.example.DoAn.dto.request.ModuleRequestDTO;
import com.example.DoAn.dto.response.ModuleResponseDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.service.IExpertModuleService;
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
@RequestMapping("/api/v1/expert/modules")
@RequiredArgsConstructor
public class ExpertModuleController {

    private final IExpertModuleService moduleService;

    private String getEmail(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken t) return t.getPrincipal().getAttribute("email");
        return principal.getName();
    }

    @Operation(summary = "Get all courses owned by the current expert")
    @GetMapping("/courses")
    public ResponseData<PageResponse<IExpertModuleService.CourseOwnedByExpertDTO>> getMyCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int limit,
            Principal principal) {
        return ResponseData.success("Danh sách khóa học",
                moduleService.getCoursesOwnedByExpert(getEmail(principal), search, status, page, limit));
    }

    @Operation(summary = "Get dashboard statistics for expert")
    @GetMapping("/stats")
    public ResponseData<IExpertModuleService.ExpertDashboardStatsDTO> getStats(Principal principal) {
        return ResponseData.success("Thống kê dashboard",
                moduleService.getDashboardStats(getEmail(principal)));
    }

    @Operation(summary = "Get all modules for a course")
    @GetMapping
    public ResponseData<List<ModuleResponseDTO>> getModules(
            @RequestParam(required = false) Integer courseId,
            Principal principal) {
        if (courseId != null) {
            return ResponseData.success("Danh sách chương",
                    moduleService.getModulesByCourse(courseId, getEmail(principal)));
        }
        return ResponseData.success("Danh sách tất cả chương",
                moduleService.getAllModulesByExpert(getEmail(principal)));
    }

    @Operation(summary = "Get all modules across all courses owned by expert")
    @GetMapping("/all")
    public ResponseData<List<ModuleResponseDTO>> getAllModules(Principal principal) {
        return ResponseData.success("Danh sách tất cả chương",
                moduleService.getAllModulesByExpert(getEmail(principal)));
    }

    @Operation(summary = "Create a new module")
    @PostMapping
    public ResponseData<ModuleResponseDTO> createModule(
            @Valid @RequestBody ModuleRequestDTO request, Principal principal) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Chương đã được tạo.",
                moduleService.createModule(request, getEmail(principal)));
    }

    @Operation(summary = "Update a module")
    @PutMapping("/{moduleId}")
    public ResponseData<ModuleResponseDTO> updateModule(
            @PathVariable Integer moduleId,
            @Valid @RequestBody ModuleRequestDTO request, Principal principal) {
        return ResponseData.success("Chương đã được cập nhật.",
                moduleService.updateModule(moduleId, request, getEmail(principal)));
    }

    @Operation(summary = "Delete a module (cascades to lessons)")
    @DeleteMapping("/{moduleId}")
    public ResponseData<Void> deleteModule(
            @PathVariable Integer moduleId, Principal principal) {
        moduleService.deleteModule(moduleId, getEmail(principal));
        return ResponseData.success("Chương đã được xóa.");
    }
}
