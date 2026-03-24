package com.example.DoAn.configuration;

import com.example.DoAn.model.*;
import com.example.DoAn.model.Module;
import com.example.DoAn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class QuizTestDataSeeder {

    /** DTO nhỏ cho câu hỏi Entry Test seed. */
    private record EntryQuestion(String skill, String cefrLevel, String content,
                                String[] options, int correctIndex, String explanation) {}

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final RegistrationRepository registrationRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initQuizTestData() {
        return args -> {
            // Always seed ENTRY_TEST (Placement Test) if not exists
            seedEntryTestIfNeeded();

            // Only seed course/test-user data once
            if (userRepository.existsByEmail("student_test@novalms.edu.vn")) {
                return;
            }
            seedCourseTestData();
        };
    }

    /** Seed ENTRY_TEST quiz if it doesn't exist yet (safe to call on every startup). */
    private void seedEntryTestIfNeeded() {
        List<Quiz> existing = quizRepository.findByQuizCategoryAndStatus("ENTRY_TEST", "PUBLISHED");
        if (!existing.isEmpty()) {
            Quiz existingQuiz = existing.get(0);
            // Kiểm tra bằng count query (tránh lazy loading issue)
            long questionCount = quizQuestionRepository.countByQuizQuizId(existingQuiz.getQuizId());
            if (questionCount > 0) {
                return; // Quiz đã có câu hỏi → không tạo lại
            }
            // Quiz chưa có câu hỏi → dùng quiz cũ để seed
            seedEntryTestQuestions(existingQuiz);
            return;
        }

        Quiz entryTest = quizRepository.save(Quiz.builder()
                .title("Bài Kiểm Tra Năng Lực Đầu Vào")
                .description("Bài test ngắn đánh giá trình độ tiếng Anh để xếp lớp phù hợp.")
                .quizCategory("ENTRY_TEST")
                .status("PUBLISHED")
                .numberOfQuestions(10)
                .passScore(new BigDecimal("40.0"))
                .timeLimitMinutes(15)
                .maxAttempts(1)
                .questionOrder("FIXED")
                .showAnswerAfterSubmit(false)
                .build());
        seedEntryTestQuestions(entryTest);
    }

    private void seedEntryTestQuestions(Quiz entryTest) {

        String[] entrySkills = {"Grammar", "Vocabulary", "Grammar", "Reading", "Grammar", "Vocabulary", "Reading", "Grammar", "Vocabulary", "Reading"};
        String[] cefrLevels = {"A1", "A1", "A2", "A2", "B1", "B1", "B1", "B2", "B2", "C1"};

        EntryQuestion[] entryQuestions = {
            new EntryQuestion("Grammar", "A1",
                "What ___ your name?",
                new String[]{"is", "am", "are", "be"},
                0, "Sử dụng 'is' với danh từ số ít (your name = nó)."),

            new EntryQuestion("Vocabulary", "A1",
                "Choose the correct translation: 'Book'",
                new String[]{"Sách", "Bút", "Giấy", "Bảng"},
                0, "'Book' = Sách."),

            new EntryQuestion("Grammar", "A2",
                "She ___ to the market yesterday.",
                new String[]{"went", "go", "goes", "going"},
                0, "'Yesterday' = quá khứ đơn → went (past tense của go)."),

            new EntryQuestion("Reading", "A2",
                "The word 'quickly' is a ___.",
                new String[]{"adverb", "noun", "adjective", "verb"},
                0, "'Quickly' bổ nghĩa cho động từ → trạng từ (adverb)."),

            new EntryQuestion("Grammar", "B1",
                "If I ___ more money, I would travel the world.",
                new String[]{"had", "have", "has", "having"},
                0, "Cấu trúc 'If + past simple, would + V (bare infinitive)' → chủ ngữ giả định."),

            new EntryQuestion("Vocabulary", "B1",
                "Choose the word most similar in meaning to 'enormous':",
                new String[]{"huge", "tiny", "angry", "quiet"},
                0, "'Enormous' = huge = khổng lồ."),

            new EntryQuestion("Reading", "B1",
                "Which sentence is grammatically correct?",
                new String[]{"Neither the students nor the teacher was present.",
                  "Neither the students nor the teacher were present.",
                  "Neither the students nor the teacher are present.",
                  "Neither the students nor the teacher be present."},
                0, "Cấu trúc 'Neither...nor' → động từ nhất quán với danh từ gần nhất (teacher = số ít → was)."),

            new EntryQuestion("Grammar", "B2",
                "He suggested ___ the meeting until next week.",
                new String[]{"postponing", "to postpone", "postpone", "postponed"},
                0, "'Suggest + V-ing' → suggest postponing. Động từ sau suggest phải là dạng V-ing."),

            new EntryQuestion("Vocabulary", "B2",
                "The company's profits have ___ significantly over the past year.",
                new String[]{"increased", "grown up", "risen", "raised"},
                2, "'Rise' (nội động từ, không có tân ngữ) = tăng lên. 'Raise' cần tân ngữ."),

            new EntryQuestion("Reading", "C1",
                "Which option best completes the sentence?\n\nDespite ___ difficulties they faced, the team managed to meet the deadline.",
                new String[]{"the numerous", "the numerous of", "a numerous", "numerous"},
                0, "'Despite' + danh từ. 'The numerous difficulties' = danh từ đếm được số nhiều với mạo từ.")
        };

        for (int i = 0; i < entryQuestions.length; i++) {
            EntryQuestion eq = entryQuestions[i];
            Question q = questionRepository.save(Question.builder()
                    .content(eq.content)
                    .questionType("MULTIPLE_CHOICE_SINGLE")
                    .skill(eq.skill)
                    .cefrLevel(eq.cefrLevel)
                    .status("PUBLISHED")
                    .explanation(eq.explanation)
                    .build());

            for (int j = 0; j < eq.options.length; j++) {
                answerOptionRepository.save(AnswerOption.builder()
                        .title(eq.options[j])
                        .correctAnswer(j == eq.correctIndex)
                        .question(q)
                        .build());
            }

            quizQuestionRepository.save(QuizQuestion.builder()
                    .quiz(entryTest)
                    .question(q)
                    .orderIndex(i + 1)
                    .points(BigDecimal.valueOf(10))
                    .build());
        }

        System.out.println(">>> Đã seed ENTRY_TEST (Bài Kiểm Tra Đầu Vào) — 10 câu hỏi.");
    }

    /** Seed course quiz + test user data (only once). */
    private void seedCourseTestData() {
        Setting studentRole = settingRepository.findRoleByName("ROLE_STUDENT").orElse(null);
        Setting teacherRole = settingRepository.findRoleByName("ROLE_TEACHER").orElse(null);

        User student = userRepository.save(User.builder()
                .email("student_test@novalms.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .fullName("Student Test")
                .role(studentRole)
                .status("Active")
                .authProvider("LOCAL")
                .build());

        User teacher = userRepository.save(User.builder()
                .email("teacher_test@novalms.edu.vn")
                .password(passwordEncoder.encode("123456"))
                .fullName("Teacher Test")
                .role(teacherRole)
                .status("Active")
                .authProvider("LOCAL")
                .build());

        Setting courseCategory = settingRepository.save(Setting.builder()
                .name("IELTS Preparation")
                .value("IELTS")
                .settingType("COURSE_CATEGORY")
                .status("Active")
                .build());

        Course course = courseRepository.save(Course.builder()
                .courseName("IELTS 6.5+ Intensive 4 Skills")
                .title("Khóa học IELTS Toàn Diện 4 Kỹ Năng")
                .description("Luyện thi IELTS chuyên sâu, master Listening, Reading, Writing, Speaking.")
                .expert(teacher)
                .category(courseCategory)
                .status("Published")
                .price(1500000.0)
                .build());

        Clazz clazz = classRepository.save(Clazz.builder()
                .course(course)
                .teacher(teacher)
                .className("IELTS-K15")
                .status("Open")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusMonths(3))
                .build());

        registrationRepository.save(Registration.builder()
                .user(student)
                .clazz(clazz)
                .course(course)
                .status("Approved")
                .registrationPrice(BigDecimal.valueOf(1500000.0))
                .build());

        Module module = moduleRepository.save(Module.builder()
                .moduleName("Chương 1: Reading Fundamentals")
                .orderIndex(1)
                .course(course)
                .build());

        Lesson videoLesson = lessonRepository.save(Lesson.builder()
                .lessonName("Chiến thuật Skimming & Scanning")
                .type("VIDEO")
                .duration("15:00")
                .orderIndex(1)
                .module(module)
                .build());

        Lesson quizLesson = lessonRepository.save(Lesson.builder()
                .lessonName("IELTS Reading Mini-Test 1")
                .type("QUIZ")
                .duration("20:00")
                .orderIndex(2)
                .module(module)
                .build());

        Quiz quiz = quizRepository.save(Quiz.builder()
                .title("IELTS Reading Mini-Test 1")
                .description("Bài test kiểm tra kỹ năng Skimming & Scanning (Match headings, T/F/NG)")
                .quizCategory("COURSE_QUIZ")
                .course(course)
                .clazz(clazz)
                .user(teacher)
                .status("PUBLISHED")
                .numberOfQuestions(30)
                .passScore(new BigDecimal("50.0"))
                .timeLimitMinutes(60)
                .maxAttempts(3)
                .questionOrder("RANDOM")
                .showAnswerAfterSubmit(true)
                .build());

        quizLesson.setQuiz_id(quiz.getQuizId());
        lessonRepository.save(quizLesson);

        String[] questionTypes = {"MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK", "MATCHING", "WRITING", "SPEAKING"};
        Random random = new Random();

        for (int i = 1; i <= 30; i++) {
            String type = questionTypes[random.nextInt(questionTypes.length)];
            String skill = type.equals("SPEAKING") ? "Speaking"
                    : (type.equals("WRITING") ? "Writing"
                    : (random.nextBoolean() ? "Reading" : "Listening"));

            Question q = questionRepository.save(Question.builder()
                    .content("English Practice Question " + i + " - Type: " + type + " (IELTS Section)")
                    .questionType(type)
                    .skill(skill)
                    .cefrLevel("C1")
                    .status("PUBLISHED")
                    .build());

            if ("MULTIPLE_CHOICE_SINGLE".equals(type)) {
                answerOptionRepository.save(AnswerOption.builder().title("Option A").correctAnswer(true).question(q).build());
                answerOptionRepository.save(AnswerOption.builder().title("Option B").correctAnswer(false).question(q).build());
                answerOptionRepository.save(AnswerOption.builder().title("Option C").correctAnswer(false).question(q).build());
                answerOptionRepository.save(AnswerOption.builder().title("Option D").correctAnswer(false).question(q).build());
            } else if ("MULTIPLE_CHOICE_MULTI".equals(type)) {
                answerOptionRepository.save(AnswerOption.builder().title("Option 1").correctAnswer(true).question(q).build());
                answerOptionRepository.save(AnswerOption.builder().title("Option 2").correctAnswer(false).question(q).build());
                answerOptionRepository.save(AnswerOption.builder().title("Option 3").correctAnswer(true).question(q).build());
            } else if ("FILL_IN_BLANK".equals(type)) {
                answerOptionRepository.save(AnswerOption.builder().title("target_word").correctAnswer(true).question(q).build());
            } else if ("MATCHING".equals(type)) {
                answerOptionRepository.save(AnswerOption.builder().title("Item 1").matchTarget("Description 1").correctAnswer(true).question(q).build());
                answerOptionRepository.save(AnswerOption.builder().title("Item 2").matchTarget("Description 2").correctAnswer(true).question(q).build());
            } else {
                answerOptionRepository.save(AnswerOption.builder().title("Sample perfect answer / rubric criterion.").correctAnswer(true).question(q).build());
            }

            quizQuestionRepository.save(QuizQuestion.builder()
                    .quiz(quiz)
                    .question(q)
                    .orderIndex(i)
                    .points(BigDecimal.valueOf(10))
                    .build());
        }

        System.out.println(">>> Đã seed Course Quiz (IELTS Mini-Test) — 30 câu hỏi.");
        System.out.println(">>> Test user: student_test@novalms.edu.vn / 123456");
    }
}
