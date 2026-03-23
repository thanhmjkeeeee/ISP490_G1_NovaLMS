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
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class QuizTestDataSeeder {

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
            // Only seed if test data doesn't exist to avoid duplicating on every startup
            if (userRepository.existsByEmail("student_test@novalms.edu.vn")) {
                return;
            }

            // 1. Get Existing Roles 
            Setting studentRole = settingRepository.findRoleByName("ROLE_STUDENT").orElse(null);
            Setting teacherRole = settingRepository.findRoleByName("ROLE_TEACHER").orElse(null);
            
            // 2. Populate Users (Student & Teacher)
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

            // 3. Create Course Category Setting
            Setting courseCategory = settingRepository.save(Setting.builder()
                    .name("IELTS Preparation")
                    .value("IELTS")
                    .settingType("COURSE_CATEGORY")
                    .status("Active")
                    .build());

            // 4. Create an academic Course 
            Course course = courseRepository.save(Course.builder()
                    .courseName("IELTS 6.5+ Intensive 4 Skills")
                    .title("Khóa học IELTS Toàn Diện 4 Kỹ Năng")
                    .description("Luyện thi IELTS chuyên sâu, master Listening, Reading, Writing, Speaking.")
                    .expert(teacher)
                    .category(courseCategory)
                    .status("Published")
                    .price(1500000.0)
                    .build());

            // 5. Build Class constraints 
            Clazz clazz = classRepository.save(Clazz.builder()
                    .course(course)
                    .teacher(teacher)
                    .className("IELTS-K15")
                    .status("Open")
                    .startDate(LocalDateTime.now().minusDays(1))
                    .endDate(LocalDateTime.now().plusMonths(3))
                    .build());

            // 6. Grant Student Registration allowing system-wide access
            registrationRepository.save(Registration.builder()
                    .user(student)
                    .clazz(clazz)
                    .course(course)
                    .status("Approved")
                    .registrationPrice(BigDecimal.valueOf(1500000.0))
                    .build());

            // 7. Organize Module and Lessons
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

            // 8. Construct Quiz Entity
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
                    
            // Mutate Lesson attaching the Quiz component
            quizLesson.setQuiz_id(quiz.getQuizId());
            lessonRepository.save(quizLesson);

            // 9. Frame 30 Randomized Test Questions
            String[] questionTypes = {"MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK", "MATCHING", "WRITING", "SPEAKING"};
            Random random = new Random();

            for (int i = 1; i <= 30; i++) {
                String type = questionTypes[random.nextInt(questionTypes.length)];
                String skill = type.equals("SPEAKING") ? "Speaking" : (type.equals("WRITING") ? "Writing" : (random.nextBoolean() ? "Reading" : "Listening"));

                Question q = questionRepository.save(Question.builder()
                        .content("English Practice Question " + i + " - Type: " + type + " (IELTS Section)")
                        .questionType(type)
                        .skill(skill)
                        .cefrLevel("C1")
                        .status("PUBLISHED")
                        .build());

                // Create suitable AnswerOptions based on type
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
                    // For WRITING or SPEAKING, just supply a sample ideal rubric AnswerOption
                    answerOptionRepository.save(AnswerOption.builder().title("Sample perfect answer / rubric criterion.").correctAnswer(true).question(q).build());
                }

                // Map to Quiz wrapper Entity 
                quizQuestionRepository.save(QuizQuestion.builder()
                        .quiz(quiz)
                        .question(q)
                        .orderIndex(i)
                        .points(BigDecimal.valueOf(10))
                        .build());
            }

            System.out.println(">>> Đã khởi tạo dữ liệu mẫu bài kiểm tra gồm 30 câu hỏi random (Học viên: student_test@novalms.edu.vn / 123456)");
        };
    }
}
