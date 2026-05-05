package com.example.DoAn.repository;

import com.example.DoAn.model.Clazz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Integer> {

    // Lấy tất cả lớp mà giáo viên (theo email) đang phụ trách
    @Query("SELECT c FROM Clazz c JOIN FETCH c.course WHERE c.teacher.userId = :teacherId")
    List<Clazz> findAllByTeacher_UserId(@Param("teacherId") Integer teacherId);

    List<Clazz> findByCourse_CourseId(Integer courseId);
    
    long countByCourse_CourseId(Integer courseId);

    @Query("SELECT c.course.courseId, COUNT(c) FROM Clazz c WHERE c.course.courseId IN :courseIds GROUP BY c.course.courseId")
    List<Object[]> countByCourseIdsBatch(@Param("courseIds") List<Integer> courseIds);

    @Query("SELECT DISTINCT c.course.courseId, t.fullName FROM Clazz c JOIN c.teacher t WHERE c.course.courseId IN :courseIds")
    List<Object[]> findTeachersByCourseIdsBatch(@Param("courseIds") List<Integer> courseIds);
}
