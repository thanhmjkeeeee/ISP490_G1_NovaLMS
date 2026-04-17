package com.example.DoAn.service.impl;

import com.example.DoAn.dto.response.ClassPublicResponseDTO;
import com.example.DoAn.dto.response.PageResponse;
import com.example.DoAn.model.Clazz;
import com.example.DoAn.repository.ClassRepository;
import com.example.DoAn.service.ClassPublicService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class ClassPublicServiceImpl implements ClassPublicService {

    private final ClassRepository classRepository;

    @Override
    public PageResponse<ClassPublicResponseDTO> getOpenClassesWithFilter(int pageNo, int pageSize, Integer categoryId,
            String keyword) {
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("classId").ascending());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateMin = now.minusDays(7);
        LocalDateTime startDateMax = now.plusDays(90);

        Specification<Clazz> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), "Open"));

            // Filter classes starting within 7 days from now (past or future)
            predicates.add(cb.between(root.get("startDate"), startDateMin, startDateMax));

            if (categoryId != null && categoryId > 0) {
                predicates.add(cb.equal(root.get("course").get("category").get("settingId"), categoryId));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = "%" + keyword.toLowerCase().trim() + "%";
                Predicate courseMatch = cb.like(cb.lower(root.get("course").get("courseName")), searchKeyword);
                Predicate teacherMatch = cb.like(cb.lower(root.get("teacher").get("fullName")), searchKeyword);
                predicates.add(cb.or(courseMatch, teacherMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Clazz> classPage = classRepository.findAll(spec, pageable);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        List<ClassPublicResponseDTO> dtoList = classPage.getContent().stream()
                .map(clazz -> ClassPublicResponseDTO.builder()
                        .classId(clazz.getClassId())
                        .courseId(clazz.getCourse() != null ? clazz.getCourse().getCourseId() : null)
                        .courseTitle(clazz.getCourse() != null ? clazz.getCourse().getCourseName() : "N/A")
                        .categoryName((clazz.getCourse() != null && clazz.getCourse().getCategory() != null)
                                ? clazz.getCourse().getCategory().getName()
                                : "N/A")
                        .className(clazz.getClassName())
                        .teacherName(clazz.getTeacher() != null ? clazz.getTeacher().getFullName() : "Đang cập nhật")
                        .schedule(clazz.getSchedule())
                        .slotTime(clazz.getSlotTime())
                        .startDate(
                                clazz.getStartDate() != null ? clazz.getStartDate().format(formatter) : "Đang cập nhật")
                        .build())
                .collect(Collectors.toList());

        return PageResponse.<ClassPublicResponseDTO>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalPages(classPage.getTotalPages())
                .totalElements(classPage.getTotalElements())
                .items(dtoList)
                .build();
    }
}