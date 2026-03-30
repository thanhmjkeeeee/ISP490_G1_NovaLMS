package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ModuleRequestDTO;
import com.example.DoAn.dto.response.ModuleResponseDTO;
import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.*;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.*;
import com.example.DoAn.service.IExpertModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertModuleServiceImpl implements IExpertModuleService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponseDTO> getModulesByCourse(Integer courseId, String email) {
        validateExpertOwnsCourse(email, courseId);
        List<Module> modules = moduleRepository.findByCourse_CourseIdOrderByOrderIndexAsc(courseId);
        return modules.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ModuleResponseDTO createModule(ModuleRequestDTO request, String email) {
        validateExpertOwnsCourse(email, request.getCourseId());
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học."));

        List<Module> existing = moduleRepository.findByCourse_CourseIdOrderByOrderIndexAsc(request.getCourseId());
        int nextOrder = existing.stream()
                .mapToInt(m -> m.getOrderIndex() != null ? m.getOrderIndex() : 0)
                .max().orElse(0) + 1;

        int orderIndex = request.getOrderIndex() != null ? request.getOrderIndex() : nextOrder;

        Module module = Module.builder()
                .course(course)
                .moduleName(request.getModuleName())
                .orderIndex(orderIndex)
                .build();

        moduleRepository.save(module);
        return toResponseDTO(module);
    }

    @Override
    @Transactional
    public ModuleResponseDTO updateModule(Integer moduleId, ModuleRequestDTO request, String email) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương."));

        validateExpertOwnsCourse(email, module.getCourse().getCourseId());

        if (request.getCourseId() != null && !request.getCourseId().equals(module.getCourse().getCourseId())) {
            validateExpertOwnsCourse(email, request.getCourseId());
            Course newCourse = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học."));
            module.setCourse(newCourse);
        }

        if (request.getModuleName() != null) module.setModuleName(request.getModuleName());
        if (request.getOrderIndex() != null) module.setOrderIndex(request.getOrderIndex());

        moduleRepository.save(module);
        return toResponseDTO(module);
    }

    @Override
    @Transactional
    public void deleteModule(Integer moduleId, String email) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương."));
        validateExpertOwnsCourse(email, module.getCourse().getCourseId());
        // Cascade: lessons are deleted by DB CASCADE (module_ibfk_1 ON DELETE CASCADE)
        moduleRepository.delete(module);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseOwnedByExpertDTO> getCoursesOwnedByExpert(String email) {
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));
        List<Course> courses = courseRepository.findByExpertUserId(expert.getUserId());
        return courses.stream().map(c -> {
            CourseOwnedByExpertDTO dto = new CourseOwnedByExpertDTO();
            dto.setCourseId(c.getCourseId());
            dto.setCourseName(c.getCourseName());
            dto.setStatus(c.getStatus());
            dto.setCategoryName(c.getCategory() != null ? c.getCategory().getName() : null);
            
            // Thêm thống kê số lượng
            dto.setModuleCount(moduleRepository.countByCourse_CourseId(c.getCourseId()));
            dto.setLessonCount(lessonRepository.countByModuleCourse_CourseId(c.getCourseId()));
            dto.setQuestionCount(questionRepository.countByModule_Course_CourseId(c.getCourseId()));
            
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ExpertDashboardStatsDTO getDashboardStats(String email) {
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));
        Integer userId = expert.getUserId();

        return ExpertDashboardStatsDTO.builder()
                .totalCourses((long) courseRepository.findByExpertUserId(userId).size())
                .totalModules(moduleRepository.countByCourse_Expert_UserId(userId))
                .totalLessons(lessonRepository.countByModule_Course_Expert_UserId(userId))
                .totalQuestions(questionRepository.countByUser_UserId(userId))
                .build();
    }

    private ModuleResponseDTO toResponseDTO(Module module) {
        int lessonCount = lessonRepository.findByModule_ModuleIdOrderByOrderIndexAsc(module.getModuleId()).size();
        int questionCount = questionRepository.findByModuleModuleId(module.getModuleId()).size();
        return ModuleResponseDTO.builder()
                .moduleId(module.getModuleId())
                .courseId(module.getCourse().getCourseId())
                .moduleName(module.getModuleName())
                .orderIndex(module.getOrderIndex())
                .lessonCount(lessonCount)
                .questionCount(questionCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleResponseDTO> getAllModulesByExpert(String email) {
        List<CourseOwnedByExpertDTO> courses = getCoursesOwnedByExpert(email);
        return courses.stream()
                .flatMap(c -> {
                    List<Module> modules = moduleRepository.findByCourse_CourseIdOrderByOrderIndexAsc(c.getCourseId());
                    return modules.stream().map(this::toResponseDTO);
                })
                .collect(Collectors.toList());
    }

    private void validateExpertOwnsCourse(String email, Integer courseId) {
        User expert = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyên gia."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học."));
        if (course.getExpert() == null || !course.getExpert().getUserId().equals(expert.getUserId())) {
            throw new ResourceNotFoundException("Bạn không có quyền quản lý khóa học này.");
        }
    }
}
