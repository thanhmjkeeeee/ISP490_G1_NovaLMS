package com.example.DoAn.service;

import com.example.DoAn.dto.response.*;
import com.example.DoAn.model.*;
import com.example.DoAn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final com.example.DoAn.repository.SessionQuizRepository sessionQuizRepository;

    @Transactional(readOnly = true)
    public ResponseData<PageResponse<MyClassDTO>> getMyClasses(String email, String keyword, String status, int page, int size) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
            String classStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

            Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("registrationTime").descending());

            Page<Registration> registrationPage = registrationRepository.findMyClassesWithFilters(user.getUserId(), searchKeyword, classStatus, pageable);

            List<Integer> classIds = registrationPage.getContent().stream()
                    .map(r -> r.getClazz().getClassId())
                    .collect(Collectors.toList());

            // Tránh lỗi khi list class rỗng
            Map<Integer, Long> sessionCountMap = new HashMap<>();
            if (!classIds.isEmpty()) {
                for(Integer cid : classIds) {
                    sessionCountMap.put(cid, (long) classSessionRepository.countByClassId(cid));
                }
            }

            // 3. MAP SANG DTO
            List<MyClassDTO> classes = registrationPage.getContent().stream().map(reg -> {
                Clazz clazz = reg.getClazz();
                Course course = reg.getCourse();

                int studentCount = (clazz.getRegistrations() != null) ? clazz.getRegistrations().size() : 0;

                return MyClassDTO.builder()
                        .classId(clazz.getClassId())
                        .className(clazz.getClassName())
                        .courseId(course.getCourseId())
                        .courseName(course.getCourseName() != null ? course.getCourseName() : course.getTitle())
                        .courseImage(course.getImageUrl())
                        .teacherName(clazz.getTeacher() != null ? clazz.getTeacher().getFullName() : null)
                        .schedule(clazz.getSchedule())
                        .slotTime(clazz.getSlotTime())
                        .status(clazz.getStatus())
                        .studentCount(studentCount)
                        .sessionCount(sessionCountMap.getOrDefault(clazz.getClassId(), 0L).intValue())
                        .build();
            }).collect(Collectors.toList());

            PageResponse<MyClassDTO> pageResponse = PageResponse.<MyClassDTO>builder()
                    .items(classes)
                    .pageNo(registrationPage.getNumber())
                    .pageSize(registrationPage.getSize())
                    .totalElements((int) registrationPage.getTotalElements())
                    .totalPages(registrationPage.getTotalPages())
                    .last(registrationPage.isLast())
                    .build();

            return ResponseData.success("Danh sách lớp học", pageResponse);
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
            Optional<Registration> regOpt = registrationRepository.findByUser_EmailAndClazz_ClassIdAndStatus(email, classId, "Approved");            if (regOpt.isEmpty()) {
                return ResponseData.error(403, "Bạn chưa đăng ký lớp học này");
            }
            Registration reg = regOpt.get();

            Clazz clazz = reg.getClazz();

            List<QuizResult> userResults = quizResultRepository.findByUser_Email(email);
            Map<Integer, List<QuizResult>> resultsByQuiz = userResults.stream()
                    .collect(Collectors.groupingBy(r -> r.getQuiz().getQuizId()));

            // Sessions — gắn quiz info vào mỗi session
            List<ClassSession> sessions = classSessionRepository.findByClazzClassIdOrderBySessionNumberAsc(classId);
            List<ClassSessionDTO> sessionDTOs = sessions.stream().map(s -> {
                List<com.example.DoAn.model.SessionQuiz> sqList = sessionQuizRepository.findBySessionSessionIdOrderByOrderIndexAsc(s.getSessionId());

                List<SessionQuizInfoDTO> quizInfoList = sqList.stream().map(sq -> {
                    Quiz q = sq.getQuiz();
                    List<QuizResult> qResults = resultsByQuiz.getOrDefault(q.getQuizId(), List.of());
                    boolean qHasAttempted = !qResults.isEmpty();
                    int qAttemptCount = qResults.size();
                    Integer qBestScore = qResults.stream()
                            .map(QuizResult::getScore)
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(null);
                    return SessionQuizInfoDTO.builder()
                            .quizId(q.getQuizId())
                            .title(q.getTitle())
                            .description(q.getDescription())
                            .timeLimitMinutes(q.getTimeLimitMinutes())
                            .passScore(q.getPassScore())
                            .maxAttempts(q.getMaxAttempts())
                            .status(q.getStatus())
                            .isOpen(sq.getIsOpen())
                            .hasAttempted(qHasAttempted)
                            .attemptCount(qAttemptCount)
                            .bestScore(qBestScore != null ? java.math.BigDecimal.valueOf(qBestScore) : null)
                            .build();
                }).toList();

                boolean allOpen = !quizInfoList.isEmpty() && quizInfoList.stream()
                        .allMatch(qi -> Boolean.TRUE.equals(qi.getIsOpen()));

                // Legacy single quiz support
                Quiz legacyQuiz = s.getQuiz();
                Integer legacyQuizId = legacyQuiz != null ? legacyQuiz.getQuizId() : null;
                List<QuizResult> legacyResults = legacyQuizId != null ? resultsByQuiz.getOrDefault(legacyQuizId, List.of()) : List.of();
                boolean legacyHasAttempted = !legacyResults.isEmpty();
                Integer legacyBestScore = legacyResults.stream()
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
                        .quizzes(quizInfoList)
                        .isAllOpen(allOpen)
                        // Legacy fields (for backward compat)
                        .legacyQuizId(legacyQuizId)
                        .quizId(legacyQuizId)
                        .quizTitle(legacyQuiz != null ? legacyQuiz.getTitle() : null)
                        .quizDescription(legacyQuiz != null ? legacyQuiz.getDescription() : null)
                        .timeLimitMinutes(legacyQuiz != null ? legacyQuiz.getTimeLimitMinutes() : null)
                        .passScore(legacyQuiz != null ? legacyQuiz.getPassScore() : null)
                        .maxAttempts(legacyQuiz != null ? legacyQuiz.getMaxAttempts() : null)
                        .quizStatus(legacyQuiz != null ? legacyQuiz.getStatus() : null)
                        .hasAttempted(legacyHasAttempted)
                        .attemptCount(legacyResults.size())
                        .bestScore(legacyBestScore != null ? java.math.BigDecimal.valueOf(legacyBestScore) : null)
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
