package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ClassRequestDTO;
import com.example.DoAn.dto.response.ClassDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.IClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassServiceImpl implements IClassService {

    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    public Integer saveClass(ClassRequestDTO request) {
        Clazz clazz = Clazz.builder()
                .className(request.getClassName())
                .course(courseRepository.findById(request.getCourseId()).orElse(null))
                .teacher(request.getTeacherId() != null ? userRepository.findById(request.getTeacherId()).orElse(null) : null)
                .startDate(request.getStartDate() != null && !request.getStartDate().isEmpty() ? LocalDateTime.parse(request.getStartDate()) : null)
                .endDate(request.getEndDate() != null && !request.getEndDate().isEmpty() ? LocalDateTime.parse(request.getEndDate()) : null)
                .status(request.getStatus() != null ? request.getStatus() : "Pending")
                .schedule(request.getSchedule())
                .slotTime(request.getSlotTime())
                .build();
        classRepository.save(clazz);
        log.info("Class created successfully: {}", clazz.getClassName());
        return clazz.getClassId();
    }

    @Override
    public void updateClass(Integer id, ClassRequestDTO request) {
        Clazz clazz = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));

        clazz.setClassName(request.getClassName());
        clazz.setCourse(courseRepository.findById(request.getCourseId()).orElse(null));
        clazz.setTeacher(request.getTeacherId() != null ? userRepository.findById(request.getTeacherId()).orElse(null) : null);

        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            clazz.setStartDate(LocalDateTime.parse(request.getStartDate()));
        }

        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            clazz.setEndDate(LocalDateTime.parse(request.getEndDate()));
        }

        clazz.setStatus(request.getStatus());
        clazz.setSchedule(request.getSchedule());
        clazz.setSlotTime(request.getSlotTime());

        classRepository.save(clazz);
        log.info("Class updated successfully, id={}", id);
    }

    @Override
    public void deleteClass(Integer id) {
        classRepository.deleteById(id);
        log.info("Class deleted, id={}", id);
    }

    @Override
    public ClassDetailResponse getClassById(Integer id) {
        Clazz clazz = classRepository.findById(id).orElseThrow(() -> new RuntimeException("Class not found"));
        return mapToResponse(clazz);
    }

    @Override
    public PageResponse<ClassDetailResponse> getAllClasses(int pageNo, int pageSize) {
        Page<Clazz> page = classRepository.findAll(PageRequest.of(pageNo, pageSize));
        List<ClassDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<ClassDetailResponse>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages())
                .items(list)
                .build();
    }

    private ClassDetailResponse mapToResponse(Clazz clazz) {
        return ClassDetailResponse.builder()
                .classId(clazz.getClassId())
                .className(clazz.getClassName() != null ? clazz.getClassName() : "N/A")
                .courseId(clazz.getCourse() != null ? clazz.getCourse().getCourseId() : null)
                .courseName(clazz.getCourse() != null ? clazz.getCourse().getCourseName() : "N/A")
                .teacherId(clazz.getTeacher() != null ? clazz.getTeacher().getUserId() : null)
                .teacherName(clazz.getTeacher() != null ? clazz.getTeacher().getFullName() : "Not assigned")
                .startDate(clazz.getStartDate() != null ? clazz.getStartDate().toString() : "")
                .endDate(clazz.getEndDate() != null ? clazz.getEndDate().toString() : "")
                .status(clazz.getStatus() != null ? clazz.getStatus() : "Pending")
                .schedule(clazz.getSchedule() != null ? clazz.getSchedule() : "N/A")
                .slotTime(clazz.getSlotTime() != null ? clazz.getSlotTime() : "N/A")
                .build();
    }
}