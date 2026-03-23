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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
@RequiredArgsConstructor
public class MasterDataSeeder {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initMasterData() {
        return args -> {
            if (userRepository.existsByEmail("expert1@novalms.edu.vn")) {
                return;
            }

            // 1. Get Roles
            Setting expertRole = settingRepository.findRoleByName("ROLE_EXPERT")
                    .orElseGet(() -> settingRepository.save(Setting.builder().name("ROLE_EXPERT").value("EXPERT").settingType("USER_ROLE").status("Active").build()));
            Setting teacherRole = settingRepository.findRoleByName("ROLE_TEACHER")
                    .orElseGet(() -> settingRepository.save(Setting.builder().name("ROLE_TEACHER").value("TEACHER").settingType("USER_ROLE").status("Active").build()));
            Setting studentRole = settingRepository.findRoleByName("ROLE_STUDENT")
                    .orElseGet(() -> settingRepository.save(Setting.builder().name("ROLE_STUDENT").value("STUDENT").settingType("USER_ROLE").status("Active").build()));

            // 2. Create 2 Experts, 2 Teachers, 2 Students
            List<User> experts = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                experts.add(userRepository.save(User.builder()
                        .email("expert" + i + "@novalms.edu.vn")
                        .password(passwordEncoder.encode("123456"))
                        .fullName("Expert Name " + i)
                        .role(expertRole).status("Active").authProvider("LOCAL").build()));
            }

            List<User> teachers = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                teachers.add(userRepository.save(User.builder()
                        .email("teacher" + i + "@novalms.edu.vn")
                        .password(passwordEncoder.encode("123456"))
                        .fullName("Teacher Name " + i)
                        .role(teacherRole).status("Active").authProvider("LOCAL").build()));
            }

            for (int i = 1; i <= 2; i++) {
                userRepository.save(User.builder()
                        .email("student" + i + "@novalms.edu.vn")
                        .password(passwordEncoder.encode("123456"))
                        .fullName("Student Name " + i)
                        .role(studentRole).status("Active").authProvider("LOCAL").build());
            }

            // 3. Category
            Setting category = settingRepository.save(Setting.builder()
                    .name("General English")
                    .value("GEN")
                    .settingType("COURSE_CATEGORY")
                    .status("Active")
                    .build());

            // 4. Create 3 Courses
            Random random = new Random();
            String[] questionTypes = {"MULTIPLE_CHOICE_SINGLE", "MULTIPLE_CHOICE_MULTI", "FILL_IN_BLANK", "MATCHING"};

            String[] levels = {"A1", "A2", "B1", "B2", "C1", "C2"};
            for (int c = 1; c <= 3; c++) {
                User leadExpert = experts.get(random.nextInt(experts.size()));
                Course course = courseRepository.save(Course.builder()
                        .courseName("Course Title " + c)
                        .title("Mastering English " + c)
                        .description("Comprehensive course for leveling up your skills.")
                        .expert(leadExpert)
                        .category(category)
                        .levelTag(levels[random.nextInt(levels.length)])
                        .status("Published")
                        .price(1000000.0 * c)
                        .build());

                // 5. Each Course has 4 Modules
                for (int m = 1; m <= 4; m++) {
                    Module module = moduleRepository.save(Module.builder()
                            .moduleName("Module " + m + " of Course " + c)
                            .orderIndex(m)
                            .course(course)
                            .build());

                    // 6. Each Module has 4 Lessons
                    for (int l = 1; l <= 4; l++) {
                        lessonRepository.save(Lesson.builder()
                                .lessonName("Lesson " + l + " - " + module.getModuleName())
                                .type(l % 2 == 0 ? "VIDEO" : "READING")
                                .orderIndex(l)
                                .module(module)
                                .build());
                    }
                }

                // 7. Each Course has 1 Quiz
                User quizCreator = teachers.get(random.nextInt(teachers.size()));
                Quiz quiz = Quiz.builder()
                        .title("Final Quiz for " + course.getCourseName())
                        .description("Complete this quiz to test your module knowledge.")
                        .quizCategory("COURSE_QUIZ")
                        .course(course)
                        .user(quizCreator)
                        .status("PUBLISHED")
                        .numberOfQuestions(30)
                        .passScore(new BigDecimal("50.0"))
                        .timeLimitMinutes(60)
                        .questionOrder("RANDOM")
                        .showAnswerAfterSubmit(true)
                        .build();
                quiz = quizRepository.save(quiz);

                // 8. Each Quiz has 30 Questions
                for (int qNum = 1; qNum <= 30; qNum++) {
                    String type = questionTypes[random.nextInt(questionTypes.length)];
                    Question q = questionRepository.save(Question.builder()
                            .content("Question " + qNum + " for Quiz " + c + " (Type: " + type + ")")
                            .questionType(type)
                            .skill("General")
                            .cefrLevel("B1")
                            .status("PUBLISHED")
                            .build());

                    // Options
                    if ("MULTIPLE_CHOICE_SINGLE".equals(type)) {
                        answerOptionRepository.save(AnswerOption.builder().title("Correct Option").correctAnswer(true).question(q).build());
                        answerOptionRepository.save(AnswerOption.builder().title("Wrong Option").correctAnswer(false).question(q).build());
                    } else if ("MULTIPLE_CHOICE_MULTI".equals(type)) {
                        answerOptionRepository.save(AnswerOption.builder().title("Option 1 (Correct)").correctAnswer(true).question(q).build());
                        answerOptionRepository.save(AnswerOption.builder().title("Option 2 (Correct)").correctAnswer(true).question(q).build());
                        answerOptionRepository.save(AnswerOption.builder().title("Option 3").correctAnswer(false).question(q).build());
                    } else if ("FILL_IN_BLANK".equals(type)) {
                        answerOptionRepository.save(AnswerOption.builder().title("answer").correctAnswer(true).question(q).build());
                    } else if ("MATCHING".equals(type)) {
                        answerOptionRepository.save(AnswerOption.builder().title("A").matchTarget("1").correctAnswer(true).question(q).build());
                        answerOptionRepository.save(AnswerOption.builder().title("B").matchTarget("2").correctAnswer(true).question(q).build());
                    }

                    quizQuestionRepository.save(QuizQuestion.builder()
                            .quiz(quiz).question(q).orderIndex(qNum).points(BigDecimal.valueOf(1)).build());
                }
            }

            System.out.println(">>> MasterDataSeeder completed: 2 Experts, 2 Students, 2 Teachers, 3 Courses created with Quizzes and Lessons.");
        };
    }
}
