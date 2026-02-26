package com.example.DoAn.service;

import com.example.DoAn.dto.MyCourseDTO;
import com.example.DoAn.model.Registration;
import com.example.DoAn.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final RegistrationRepository registrationRepository;

    public Page<MyCourseDTO> getMyCourses(Integer userId, String keyword, Integer categoryId, Pageable pageable) {

        // Lấy data thô từ Database
        Page<Registration> regPage = registrationRepository.findMyCoursesWithFilters(userId, keyword, categoryId, pageable);

        // Map sang DTO sạch sẽ
        List<MyCourseDTO> dtoList = regPage.getContent().stream().map(reg ->
                MyCourseDTO.builder()
                        .courseId(reg.getCourse().getCourseId())
                        .title(reg.getCourse().getTitle())
                        .description(reg.getCourse().getDescription())
                        .imageUrl(reg.getCourse().getImageUrl())
                        .className(reg.getClazz() != null ? reg.getClazz().getClassName() : "N/A")
                        .build()
        ).collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, regPage.getTotalElements());
    }
}