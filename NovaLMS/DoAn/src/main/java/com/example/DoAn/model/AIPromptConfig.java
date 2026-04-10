package com.example.DoAn.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_prompt_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIPromptConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String bucket; // beginner, intermediate, advanced

    @Column(columnDefinition = "TEXT")
    private String bloomInstruction;

    @Column(columnDefinition = "TEXT")
    private String grammarFocus; // Stored as JSON or comma-separated string

    @Column(columnDefinition = "TEXT")
    private String questionTypesRatio; // Stored as JSON

    @Column(columnDefinition = "TEXT")
    private String skills; // Comma-separated or JSON

    private String lexicalComplexity;

    @Column(columnDefinition = "TEXT")
    private String writingConstraint;

    @Column(columnDefinition = "TEXT")
    private String speakingConstraint;
}
