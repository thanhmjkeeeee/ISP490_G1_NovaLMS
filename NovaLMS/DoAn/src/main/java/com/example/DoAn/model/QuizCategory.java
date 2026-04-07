package com.example.DoAn.model;

import java.util.Arrays;
import java.util.List;

public enum QuizCategory {
    ENTRY_TEST("ENTRY_TEST", "Bài kiểm tra đầu vào", false),
    COURSE_QUIZ("COURSE_QUIZ", "Bài kiểm tra khóa học", false),
    MODULE_QUIZ("MODULE_QUIZ", "Bài kiểm tra module", false),
    LESSON_QUIZ("LESSON_QUIZ", "Bài kiểm tra bài học", false),
    COURSE_ASSIGNMENT("COURSE_ASSIGNMENT", "Bài tập lớn khóa học", true),
    MODULE_ASSIGNMENT("MODULE_ASSIGNMENT", "Bài tập lớn module", true);

    private final String value;
    private final String label;
    private final boolean isAssignment;

    QuizCategory(String value, String label, boolean isAssignment) {
        this.value = value;
        this.label = label;
        this.isAssignment = isAssignment;
    }

    public String getValue()        { return value; }
    public String getLabel()        { return label; }
    public boolean isAssignment()    { return isAssignment; }
    public boolean isQuiz()         { return !isAssignment; }

    /** Fixed LRWS order; null for non-assignment types */
    public List<String> getSkillOrder() {
        if (!isAssignment) return null;
        return Arrays.asList("LISTENING", "READING", "SPEAKING", "WRITING");
    }

    public static QuizCategory fromValue(String value) {
        if (value == null) return null;
        for (QuizCategory c : values()) {
            if (c.value.equals(value)) return c;
        }
        return null;
    }
}
