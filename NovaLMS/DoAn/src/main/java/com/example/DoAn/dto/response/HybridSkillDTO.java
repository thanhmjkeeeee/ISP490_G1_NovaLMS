package com.example.DoAn.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HybridSkillDTO {
    private String skill;
    private int availableQuizzes;
}
