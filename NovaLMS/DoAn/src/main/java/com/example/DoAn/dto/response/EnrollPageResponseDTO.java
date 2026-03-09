package com.example.DoAn.dto.response;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollPageResponseDTO {
    private Course course;
    private List<Clazz> classes;
}