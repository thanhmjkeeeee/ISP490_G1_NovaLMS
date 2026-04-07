package com.example.DoAn.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentQuestionRequestDTO {

    @NotEmpty(message = "Question IDs cannot be empty")
    private List<Integer> questionIds;

    @NotNull(message = "Skill is required")
    private String skill; // LISTENING, READING, SPEAKING, WRITING

    private String itemType = "SINGLE"; // SINGLE or GROUP
}
