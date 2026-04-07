package com.example.DoAn.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingResponse {

    /** Overall IELTS band (0–9, có 0.5), ví dụ: 6.875 */
    private double overallBand;

    /** Điểm hiển thị (thang 10 hoặc 20), ví dụ: 17.19/20 */
    private double displayScore;

    /** Điểm tối đa (10 hoặc 20) */
    private double maxScore;

    /** Nhận xét tổng bằng tiếng Việt */
    private String feedback;

    /** VD: "Good User (7.0)" */
    private String overallBandDescriptor;

    /**
     * Map của 4 tiêu chí.
     * Key WRITING: task_achievement, lexical_resource, grammatical_range, coherence_cohesion
     * Key SPEAKING: fluency_cohesion, lexical_resource, grammatical_range, pronunciation
     */
    private Map<String, RubricCriterion> rubric;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RubricCriterion {
        /** Điểm đạt được (0–9, có 0.5) */
        private double score;
        /** Luôn = 9 */
        private double max;
        /** VD: "Band 7.0" hoặc "7.5" */
        private String bandLabel;
        /** Mô tả band đạt được (tiếng Anh, trích từ IELTS official) */
        private String bandDescription;
        /** Giải thích ngắn bằng tiếng Việt tại sao đạt mức này */
        private String aiReasoning;
    }
}
