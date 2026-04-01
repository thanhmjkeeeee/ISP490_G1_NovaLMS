package com.example.DoAn.dto.response;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuestionBankItemDTO {
    private Integer id; // Có thể là QuestionID hoặc GroupID
    private String type; // "SINGLE" hoặc "GROUP"
    private String content; // Nội dung câu lẻ hoặc nội dung passage
    private String questionType; // Loại câu hỏi (nếu là SINGLE) hoặc "PASSAGE"
    private String skill;
    private String cefrLevel;
    private String topic;
    private String status;
    private Integer subQuestionCount; // Số lượng câu hỏi con (nếu là GROUP)
    private long usedInQuizCount; // Số lần được sử dụng trong Quiz
    private java.time.LocalDateTime createdAt;

    // Danh sách câu hỏi con nếu là GROUP (sẽ trả về khi cần expand hoặc trả kèm nếu danh sách nhỏ)
    private List<QuestionBankResponseDTO> questions;
}
