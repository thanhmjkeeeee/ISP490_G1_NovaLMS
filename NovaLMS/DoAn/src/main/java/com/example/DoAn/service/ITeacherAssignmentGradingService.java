package com.example.DoAn.service;

import com.example.DoAn.dto.request.AssignmentGradingRequestDTO;
import com.example.DoAn.dto.response.AssignmentGradingDetailDTO;
import com.example.DoAn.dto.response.AssignmentGradingQueueDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ITeacherAssignmentGradingService {

    /**
     * Get the grading queue for assignment-type quizzes that the teacher has access to.
     * Filters: assignment-type quizzes only (COURSE_ASSIGNMENT or MODULE_ASSIGNMENT),
     * belonging to teacher's enrolled classes, matching optional quizId/classId.
     */
    Page<AssignmentGradingQueueDTO> getGradingQueue(
        String teacherEmail, Integer quizId, Integer classId, java.util.List<String> status, Pageable pageable);

    /**
     * Get full grading detail for a single student result.
     * Throws ResourceNotFoundException if result not found.
     */
    AssignmentGradingDetailDTO getGradingDetail(Integer resultId, String teacherEmail);

    /**
     * Submit grading for an assignment result.
     * Saves per-question scores, updates sectionScores JSON, recalculates total & pass/fail.
     */
    Double gradeAssignment(Integer resultId, AssignmentGradingRequestDTO request, String teacherEmail);
}
