package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ClassRequestDTO;
import com.example.DoAn.dto.response.ClassDetailResponse;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.dto.response.RegistrationResponseDTO;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.repository.CourseRepository;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.IClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

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
    public PageResponse<ClassDetailResponse> getAllClasses(int pageNo, int pageSize, String search, String status) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("classId").descending());

        Specification<Clazz> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by className or courseName
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim() + "%";
                Predicate classNamePredicate = cb.like(cb.lower(root.get("className")), searchPattern.toLowerCase());
                Predicate courseNamePredicate = cb.like(cb.lower(root.get("course").get("courseName")), searchPattern.toLowerCase());
                predicates.add(cb.or(classNamePredicate, courseNamePredicate));
            }

            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Clazz> page = classRepository.findAll(spec, pageable);
        List<ClassDetailResponse> list = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<ClassDetailResponse>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(page.getTotalPages())
                .totalElements((int) page.getTotalElements())
                .items(list)
                .build();
    }

    private ClassDetailResponse mapToResponse(Clazz clazz) {
        // Map registrations to DTO
        List<RegistrationResponseDTO> registrationDTOs = null;
        if (clazz.getRegistrations() != null) {
            registrationDTOs = clazz.getRegistrations().stream()
                    .map(reg -> RegistrationResponseDTO.builder()
                            .registrationId(reg.getRegistrationId())
                            .userId(reg.getUser() != null ? reg.getUser().getUserId() : null)
                            .userName(reg.getUser() != null ? reg.getUser().getFullName() : null)
                            .userEmail(reg.getUser() != null ? reg.getUser().getEmail() : null)
                            .classId(reg.getClazz() != null ? reg.getClazz().getClassId() : null)
                            .className(reg.getClazz() != null ? reg.getClazz().getClassName() : null)
                            .courseId(reg.getCourse() != null ? reg.getCourse().getCourseId() : null)
                            .courseName(reg.getCourse() != null ? reg.getCourse().getCourseName() : null)
                            .registrationTime(reg.getRegistrationTime() != null ? LocalDateTime.parse(reg.getRegistrationTime().toString()) : null)
                            .status(reg.getStatus() != null ? reg.getStatus() : "Pending")
                            .registrationPrice(reg.getRegistrationPrice())
                            .build())
                    .toList();
        }

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
                .registrations(registrationDTOs)
                .build();
    }
}