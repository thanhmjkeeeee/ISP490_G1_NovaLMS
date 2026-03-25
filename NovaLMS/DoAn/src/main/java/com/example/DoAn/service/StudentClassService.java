package com.example.DoAn.service;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentClassService {

    private final RegistrationRepository registrationRepository;
    private final ClassSessionRepository classSessionRepository;
    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ResponseData<List<MyClassDTO>> getMyClasses(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Registration> approvedRegs = registrationRepository
                    .findByUser_UserIdOrderByRegistrationTimeDesc(user.getUserId())
                    .stream()
                    .filter(r -> "Approved".equals(r.getStatus()))
                    .toList();

            List<MyClassDTO> classes = approvedRegs.stream().map(reg -> {
                Clazz clazz = reg.getClazz();
                int sessionCount = classSessionRepository.countByClassId(clazz.getClassId());

                return MyClassDTO.builder()
                        .classId(clazz.getClassId())
                        .className(clazz.getClassName())
                        .courseId(reg.getCourse().getCourseId())
                        .courseName(reg.getCourse().getTitle())
                        .courseImage(reg.getCourse().getImageUrl())
                        .teacherName(clazz.getTeacher() != null ? clazz.getTeacher().getFullName() : null)
                        .schedule(clazz.getSchedule())
                        .slotTime(clazz.getSlotTime())
                        .status(clazz.getStatus())
                        .studentCount(clazz.getRegistrations() != null ? clazz.getRegistrations().size() : 0)
                        .sessionCount(sessionCount)
                        .build();
            }).toList();

            return ResponseData.success("Danh sách lớp học", classes);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ResponseData<ClassDetailDTO> getClassDetail(String email, Integer classId) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Verify enrollment
            Optional<Registration> regOpt = registrationRepository.findByClazz_ClassIdAndStatus(classId, "Approved");
            if (regOpt.isEmpty()) {
                return ResponseData.error(403, "Bạn chưa đăng ký lớp học này");
            }
            Registration reg = regOpt.get();

            Clazz clazz = reg.getClazz();

            // Quiz attempt info
            List<QuizResult> userResults = quizResultRepository.findByUser_Email(email);
            Map<Integer, List<QuizResult>> resultsByQuiz = userResults.stream()
                    .collect(Collectors.groupingBy(r -> r.getQuiz().getQuizId()));

            // Sessions — gắn quiz info vào mỗi session
            List<ClassSession> sessions = classSessionRepository.findByClazzClassIdOrderBySessionNumberAsc(classId);
            List<ClassSessionDTO> sessionDTOs = sessions.stream().map(s -> {
                Quiz quiz = s.getQuiz();
                Integer quizId = quiz != null ? quiz.getQuizId() : null;
                List<QuizResult> quizResults = resultsByQuiz.getOrDefault(quizId, List.of());
                boolean hasAttempted = !quizResults.isEmpty();
                int attemptCount = quizResults.size();
                Integer bestScore = quizResults.stream()
                        .map(QuizResult::getScore)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(null);

                return ClassSessionDTO.builder()
                        .sessionId(s.getSessionId())
                        .sessionNumber(s.getSessionNumber())
                        .sessionDate(s.getSessionDate())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .topic(s.getTopic())
                        .notes(s.getNotes())
                        .quizId(quizId)
                        .quizTitle(quiz != null ? quiz.getTitle() : null)
                        .quizDescription(quiz != null ? quiz.getDescription() : null)
                        .timeLimitMinutes(quiz != null ? quiz.getTimeLimitMinutes() : null)
                        .passScore(quiz != null ? quiz.getPassScore() : null)
                        .maxAttempts(quiz != null ? quiz.getMaxAttempts() : null)
                        .quizStatus(quiz != null ? quiz.getStatus() : null)
                        .hasAttempted(hasAttempted)
                        .attemptCount(attemptCount)
                        .bestScore(bestScore != null ? java.math.BigDecimal.valueOf(bestScore) : null)
                        .build();
            }).toList();

            // All quizzes in class (for quizzes tab)
            List<Quiz> quizzes = quizRepository.findByClazz_ClassId(classId);
            List<ClassQuizDTO> quizDTOs = quizzes.stream().map(q -> {
                List<QuizResult> quizResults = resultsByQuiz.getOrDefault(q.getQuizId(), List.of());
                boolean hasAttempted = !quizResults.isEmpty();
                int attemptCount = quizResults.size();
                Integer bestScore = quizResults.stream()
                        .map(QuizResult::getScore)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(null);

                return ClassQuizDTO.builder()
                        .quizId(q.getQuizId())
                        .title(q.getTitle())
                        .description(q.getDescription())
                        .timeLimitMinutes(q.getTimeLimitMinutes())
                        .passScore(q.getPassScore())
                        .maxAttempts(q.getMaxAttempts())
                        .status(q.getStatus())
                        .createdAt(q.getCreatedAt())
                        .hasAttempted(hasAttempted)
                        .attemptCount(attemptCount)
                        .bestScore(bestScore != null ? java.math.BigDecimal.valueOf(bestScore) : null)
                        .build();
            }).toList();

            ClassDetailDTO dto = ClassDetailDTO.builder()
                    .classId(clazz.getClassId())
                    .className(clazz.getClassName())
                    .courseId(reg.getCourse().getCourseId())
                    .courseName(reg.getCourse().getTitle())
                    .courseImage(reg.getCourse().getImageUrl())
                    .teacherName(clazz.getTeacher() != null ? clazz.getTeacher().getFullName() : null)
                    .startDate(clazz.getStartDate())
                    .endDate(clazz.getEndDate())
                    .schedule(clazz.getSchedule())
                    .slotTime(clazz.getSlotTime())
                    .status(clazz.getStatus())
                    .studentCount(clazz.getRegistrations() != null ? clazz.getRegistrations().size() : 0)
                    .numberOfSessions(clazz.getNumberOfSessions() != null ? clazz.getNumberOfSessions() : sessionDTOs.size())
                    .sessions(sessionDTOs)
                    .quizzes(quizDTOs)
                    .build();

            return ResponseData.success("Chi tiết lớp học", dto);
        } catch (Exception e) {
            return ResponseData.error(500, e.getMessage());
        }
    }
}
