package com.example.DoAn.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class PlacementTestSubmissionDTO {
    private Integer quizId;
    private String guestName;          // optional
    private String guestEmail;         // optional
    private Map<Integer, Object> answers;
}
