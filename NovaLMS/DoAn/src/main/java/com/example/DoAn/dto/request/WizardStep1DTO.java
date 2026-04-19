package com.example.DoAn.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WizardStep1DTO {

    @NotNull(message = "Chế độ là bắt buộc")
    private String mode; // "PASSAGE_BASED" or "TOPIC_BASED"

    @NotBlank(message = "Kỹ năng là bắt buộc")
    private String skill; // "LISTENING", "READING", "WRITING", "SPEAKING"

    @NotBlank(message = "Cấp độ CEFR là bắt buộc")
    private String cefrLevel; // "A1", "A2", "B1", "B2", "C1", "C2"

    @NotBlank(message = "Chủ đề là bắt buộc")
    @Size(min = 2, max = 200, message = "Chủ đề phải từ 2 đến 200 ký tự")
    private String topic;

    private java.util.List<@Size(max = 50) String> tags; // max 10 items

    @Size(min = 10, max = 5000, message = "Passage phải từ 10 đến 5000 ký tự")
    private String passageContent; // required when mode == PASSAGE_BASED

    @Size(max = 500, message = "URL âm thanh quá dài")
    private String audioUrl;

    @Size(max = 500, message = "URL hình ảnh quá dài")
    private String imageUrl;
}
