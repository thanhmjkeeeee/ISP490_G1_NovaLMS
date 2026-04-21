package com.example.DoAn.service;

import com.example.DoAn.model.*;
import com.example.DoAn.model.Module; // Avoid confusion
import com.example.DoAn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeederService {

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    // Repositories
    private final SettingRepository settingRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final ClazzRepository clazzRepository;
    private final RegistrationRepository registrationRepository;
    private final PaymentRepository paymentRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final NotificationRepository notificationRepository;
    private final ClassSessionRepository classSessionRepository;
    private final SessionLessonRepository sessionLessonRepository;
    private final SessionQuizRepository sessionQuizRepository;
    private final RescheduleRequestRepository rescheduleRequestRepository;
    private final AssignmentSessionRepository assignmentSessionRepository;
    private final UserLearningLogRepository userLearningLogRepository;
    private final UserLessonRepository userLessonRepository;
    private final QuizAssignmentRepository quizAssignmentRepository;

    @Transactional
    public Map<String, Long> seed() {
        log.info("Starting database wipe and seeding...");

        // 1. WIPE EVERYTHING
        wipeDatabase();

        Map<String, Long> summary = new LinkedHashMap<>();

        // 2. SEED MASTER DATA (Settings/Roles)
        List<Setting> settings = seedSettings();
        summary.put("Settings", (long) settings.size());

        // 3. SEED USERS
        List<User> users = seedUsers(settings);
        summary.put("Users", (long) users.size());

        // 4. SEED COURSES
        List<Course> courses = seedCourses(users, settings);
        summary.put("Courses", (long) courses.size());

        // 5. SEED MODULES & LESSONS
        List<Module> modules = seedModules(courses);
        summary.put("Modules", (long) modules.size());

        List<Lesson> lessons = seedLessons(modules);
        summary.put("Lessons", (long) lessons.size());

        // 6. SEED CLASSES
        List<Clazz> classes = seedClasses(courses, users);
        summary.put("Classes", (long) classes.size());

        // 7. SEED REGISTRATIONS & PAYMENTS
        seedRegistrations(users, classes, courses);
        summary.put("Registrations", 30L);

        // 8. SEED QUESTIONS & GROUPS
        List<QuestionGroup> groups = seedQuestionGroups(users);
        List<Question> questions = seedQuestions(users, modules, groups);
        seedAnswerOptions(questions);
        summary.put("Questions", (long) questions.size());

        // 9. SEED QUIZZES
        seedQuizzes(courses, modules, lessons, users, questions);
        summary.put("Quizzes", 15L);

        log.info("Database seeding completed successfully.");
        return summary;
    }

    @Transactional
    public void seedFromSqlFile(String fileName) {
        log.info("Starting SQL file seeding: {}", fileName);
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

            // Try both local filesystem and classpath
            File file = new File(fileName);
            if (file.exists()) {
                populator.addScript(new FileSystemResource(file));
            } else {
                populator.addScript(new ClassPathResource(fileName));
            }

            populator.setContinueOnError(true); // Skip errors if any table already truncated/dropped
            populator.setSqlScriptEncoding("UTF-8");
            populator.execute(dataSource);
            log.info("SQL file seeding completed: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to seed from SQL file {}: {}", fileName, e.getMessage());
            throw new RuntimeException("SQL Seed failed: " + e.getMessage());
        }
    }

    private void wipeDatabase() {
        // Disable foreign key checks for clean wipe
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

        String[] tables = {
                "quiz_answer", "quiz_result", "quiz_question", "answer_option",
                "question", "question_group", "quiz_assignment", "assignment_session",
                "notification", "session_lesson", "session_quiz", "class_session",
                "payment", "registration", "user_lesson", "user_learning_log",
                "lesson_quiz_progress", "password_reset_token", "email_verification",
                "class", "lesson", "module", "quiz", "course", "user", "setting"
        };

        for (String table : tables) {
            entityManager.createNativeQuery("DELETE FROM " + table).executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE " + table + " AUTO_INCREMENT = 1").executeUpdate();
        }

        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }

    private List<Setting> seedSettings() {
        List<Setting> settings = new ArrayList<>();

        // Roles
        String[] roles = { "ROLE_ADMIN", "ROLE_TEACHER", "ROLE_EXPERT", "ROLE_STUDENT" };
        for (int i = 0; i < roles.length; i++) {
            settings.add(settingRepository.save(Setting.builder()
                    .name(roles[i].replace("ROLE_", ""))
                    .value(roles[i])
                    .settingType("USER_ROLE")
                    .status("Active")
                    .orderIndex(i + 1)
                    .build()));
        }

        // Categories
        String[] cats = { "IELTS Academic", "IELTS General", "TOEIC", "SAT Prep", "Grammar Master" };
        for (int i = 0; i < cats.length; i++) {
            settings.add(settingRepository.save(Setting.builder()
                    .name(cats[i])
                    .value("CAT_" + cats[i].toUpperCase().replace(" ", "_"))
                    .settingType("COURSE_CATEGORY")
                    .status("Active")
                    .orderIndex(i + 1)
                    .build()));
        }

        return settings;
    }

    private List<User> seedUsers(List<Setting> settings) {
        List<User> users = new ArrayList<>();
        String encodedPassword = passwordEncoder.encode("123456");

        Setting adminRole = settings.stream().filter(s -> s.getValue().equals("ROLE_ADMIN")).findFirst().get();
        Setting teacherRole = settings.stream().filter(s -> s.getValue().equals("ROLE_TEACHER")).findFirst().get();
        Setting expertRole = settings.stream().filter(s -> s.getValue().equals("ROLE_EXPERT")).findFirst().get();
        Setting studentRole = settings.stream().filter(s -> s.getValue().equals("ROLE_STUDENT")).findFirst().get();

        // 2 Admins
        users.add(userRepository.save(User.builder().fullName("Admin Boss").email("admin@novalms.com")
                .password(encodedPassword).role(adminRole).status("Active").build()));
        users.add(userRepository.save(User.builder().fullName("System Admin").email("sysadmin@novalms.com")
                .password(encodedPassword).role(adminRole).status("Active").build()));

        // 5 Experts
        for (int i = 1; i <= 5; i++) {
            users.add(userRepository.save(User.builder().fullName("Expert " + i).email("expert" + i + "@novalms.com")
                    .password(encodedPassword).role(expertRole).status("Active").build()));
        }

        // 5 Teachers
        for (int i = 1; i <= 5; i++) {
            users.add(userRepository
                    .save(User.builder().fullName("Teacher Hero " + i).email("teacher" + i + "@novalms.com")
                            .password(encodedPassword).role(teacherRole).status("Active").build()));
        }

        // 20 Students
        for (int i = 1; i <= 20; i++) {
            users.add(userRepository.save(User.builder().fullName("IELTS Learner " + i)
                    .email("student" + i + "@novalms.com").password(encodedPassword).role(studentRole).status("Active")
                    .city("Hanoi").gender(i % 2 == 0 ? "Male" : "Female").build()));
        }

        return users;
    }

    private List<Course> seedCourses(List<User> users, List<Setting> settings) {
        List<Course> courses = new ArrayList<>();
        User expert = users.stream().filter(u -> u.getRole().getValue().equals("ROLE_EXPERT")).findFirst().get();
        Setting category = settings.stream().filter(s -> s.getSettingType().equals("COURSE_CATEGORY")).findFirst()
                .get();

        String[] levels = { "A1", "A2", "B1", "B2", "C1", "C2" };
        String[] titles = {
                "Complete IELTS Academic 6.5+",
                "TOEIC Bridge for Beginners",
                "SAT Digital Foundations",
                "Business English for Professional",
                "Advanced Grammar and Writing",
                "IELTS Speaking Simulation Pack",
                "Vocabulary for Academic Purposes",
                "Listening Strategy: From Zero to Hero",
                "Critical Thinking in Reading",
                "The Art of English Communication"
        };

        for (int i = 0; i < titles.length; i++) {
            courses.add(courseRepository.save(Course.builder()
                    .title(titles[i])
                    .courseName(titles[i])
                    .expert(expert)
                    .category(category)
                    .levelTag(levels[i % levels.length])
                    .price(1000000.0 + (i * 200000))
                    .status("Active")
                    .description("Comprehensive course for mastering " + titles[i])
                    .numberOfSessions(20)
                    .build()));
        }
        return courses;
    }

    private List<Module> seedModules(List<Course> courses) {
        List<Module> modules = new ArrayList<>();
        for (Course c : courses) {
            for (int i = 1; i <= 3; i++) {
                modules.add(moduleRepository.save(Module.builder()
                        .moduleName("Chapter " + i + ": Foundations of " + c.getTitle())
                        .course(c)
                        .orderIndex(i)
                        .cefrLevel(c.getLevelTag())
                        .build()));
            }
        }
        return modules;
    }

    private List<Lesson> seedLessons(List<Module> modules) {
        List<Lesson> lessons = new ArrayList<>();
        for (Module m : modules) {
            for (int i = 1; i <= 3; i++) {
                lessons.add(lessonRepository.save(Lesson.builder()
                        .lessonName("Lesson " + i + ": Deep Dive Level " + i)
                        .module(m)
                        .orderIndex(i)
                        .type("VIDEO")
                        .videoUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                        .duration("15:00")
                        .allowDownload(true)
                        .build()));
            }
        }
        return lessons;
    }

    private List<Clazz> seedClasses(List<Course> courses, List<User> users) {
        List<Clazz> classes = new ArrayList<>();
        User teacher = users.stream().filter(u -> u.getRole().getValue().equals("ROLE_TEACHER")).findFirst().get();

        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            classes.add(clazzRepository.save(Clazz.builder()
                    .className(c.getTitle().split(" ")[0] + "-Group" + (i + 1))
                    .course(c)
                    .teacher(teacher)
                    .status("Open")
                    .startDate(LocalDateTime.now().plusDays(i))
                    .endDate(LocalDateTime.now().plusMonths(3))
                    .schedule("Mon-Wed-Fri")
                    .slotTime("18:00 - 20:00")
                    .numberOfSessions(24)
                    .build()));
        }
        return classes;
    }

    private void seedRegistrations(List<User> users, List<Clazz> classes, List<Course> courses) {
        List<User> students = users.stream().filter(u -> u.getRole().getValue().equals("ROLE_STUDENT")).toList();
        for (int i = 0; i < students.size(); i++) {
            Clazz targetClass = classes.get(i % classes.size());
            Registration reg = registrationRepository.save(Registration.builder()
                    .user(students.get(i))
                    .clazz(targetClass)
                    .course(targetClass.getCourse())
                    .registrationPrice(BigDecimal.valueOf(targetClass.getCourse().getPrice()))
                    .status("Approved")
                    .registrationTime(LocalDateTime.now().minusDays(i))
                    .build());

            paymentRepository.save(Payment.builder()
                    .registrationId(reg.getRegistrationId())
                    .amount(reg.getRegistrationPrice())
                    .payosOrderCode(System.currentTimeMillis() + i)
                    .status("PAID")
                    .description("Seed Payment Student " + i)
                    .paidAt(LocalDateTime.now())
                    .build());
        }
    }

    private List<QuestionGroup> seedQuestionGroups(List<User> users) {
        List<QuestionGroup> groups = new ArrayList<>();
        User expert = users.stream().filter(u -> u.getRole().getValue().equals("ROLE_EXPERT")).findFirst().get();

        String[] topics = { "Global Warming", "Urbanization", "Artificial Intelligence", "Healthy Lifestyle",
                "Remote Work" };
        for (int i = 0; i < topics.length; i++) {
            groups.add(questionGroupRepository.save(QuestionGroup.builder()
                    .groupContent("Read the paragraph about " + topics[i] + " and answer following questions.")
                    .skill("READING")
                    .cefrLevel("B2")
                    .topic(topics[i])
                    .status("PUBLISHED")
                    .user(expert)
                    .build()));
        }
        return groups;
    }

    private List<Question> seedQuestions(List<User> users, List<Module> modules, List<QuestionGroup> groups) {
        List<Question> questions = new ArrayList<>();
        User expert = users.stream().filter(u -> u.getRole().getValue().equals("ROLE_EXPERT")).findFirst().get();

        String[] skills = { "READING", "LISTENING", "SPEAKING", "WRITING" };
        String[] types = { "MULTIPLE_CHOICE_SINGLE", "FILL_IN_BLANK", "WRITING", "SPEAKING" };

        for (int i = 1; i <= 20; i++) {
            QuestionGroup group = (i <= 5) ? groups.get(i - 1) : null;
            String skill = (group != null) ? group.getSkill() : skills[i % skills.length];
            String type = (skill.equals("WRITING") || skill.equals("SPEAKING")) ? skill : types[i % 2];

            questions.add(questionRepository.save(Question.builder()
                    .content("Mock Question " + i + " content relating to " + skill)
                    .questionType(type)
                    .skill(skill)
                    .cefrLevel("B1")
                    .status("PUBLISHED")
                    .source("EXPERT_BANK")
                    .user(expert)
                    .questionGroup(group)
                    .module(modules.get(i % modules.size()))
                    .build()));
        }
        return questions;
    }

    private void seedAnswerOptions(List<Question> questions) {
        for (Question q : questions) {
            if (q.getQuestionType().contains("MULTIPLE_CHOICE")) {
                for (int i = 1; i <= 4; i++) {
                    answerOptionRepository.save(AnswerOption.builder()
                            .question(q)
                            .title("Option " + i)
                            .correctAnswer(i == 1)
                            .orderIndex(i)
                            .build());
                }
            }
        }
    }

    private void seedQuizzes(List<Course> courses, List<Module> modules, List<Lesson> lessons, List<User> users,
            List<Question> questions) {
        User expert = users.stream().filter(u -> u.getRole().getValue().equals("ROLE_EXPERT")).findFirst().get();
        for (int i = 0; i < 10; i++) {
            Quiz q = quizRepository.save(Quiz.builder()
                    .title("Practice Test " + (i + 1))
                    .quizCategory("COURSE_QUIZ")
                    .status("PUBLISHED")
                    .course(courses.get(i % courses.size()))
                    .module(modules.get(i % modules.size()))
                    .user(expert)
                    .timeLimitMinutes(30)
                    .passScore(BigDecimal.valueOf(70.0))
                    .maxAttempts(3)
                    .isOpen(true)
                    .build());

            // Assign some questions to quiz
            for (int k = 0; k < 5; k++) {
                quizQuestionRepository.save(QuizQuestion.builder()
                        .quiz(q)
                        .question(questions.get((i * 2 + k) % questions.size()))
                        .orderIndex(k + 1)
                        .points(BigDecimal.valueOf(2.0))
                        .build());
            }
        }
    }
}
