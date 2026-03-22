package com.example.DoAn.service;

import com.example.DoAn.dto.request.ModuleRequestDTO;
import com.example.DoAn.dto.response.ModuleResponseDTO;
import lombok.Data;

import java.util.List;

public interface IExpertModuleService {

    List<ModuleResponseDTO> getModulesByCourse(Integer courseId, String email);

    ModuleResponseDTO createModule(ModuleRequestDTO request, String email);

    ModuleResponseDTO updateModule(Integer moduleId, ModuleRequestDTO request, String email);

    void deleteModule(Integer moduleId, String email);

    List<CourseOwnedByExpertDTO> getCoursesOwnedByExpert(String email);

    List<ModuleResponseDTO> getAllModulesByExpert(String email);

    @Data
    class CourseOwnedByExpertDTO {
        private Integer courseId;
        private String courseName;
        private String categoryName;
        private String status;
    }
}
