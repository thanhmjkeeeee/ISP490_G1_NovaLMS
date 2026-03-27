package com.example.DoAn.repository;

import com.example.DoAn.model.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<Clazz, Integer>, JpaSpecificationExecutor<Clazz> {
    // Tìm các lớp theo Course ID và trạng thái (ví dụ: tìm lớp đang mở 'Open')
    List<Clazz> findByCourse_CourseIdAndStatus(Integer courseId, String status);

    List<Clazz> findByStatus(String status);
    List<Clazz> findByStatusAndCourse_Category_SettingId(String status, Integer categoryId);

    @Query("SELECT c FROM Clazz c WHERE c.teacher.userId = :teacherId AND LOWER(c.schedule) = LOWER(:schedule)" +
            " AND (:excludeClassId IS NULL OR c.classId <> :excludeClassId)")
    List<Clazz> findByTeacherAndSchedule(
            @Param("teacherId") Integer teacherId,
            @Param("schedule") String schedule,
            @Param("excludeClassId") Integer excludeClassId
    );

    boolean existsByClassNameIgnoreCaseAndClassIdNot(String className, Integer classId);

    boolean existsByClassNameIgnoreCase(String className);
}