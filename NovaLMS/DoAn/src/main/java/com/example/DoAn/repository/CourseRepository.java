package com.example.DoAn.repository;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer>, JpaSpecificationExecutor<Course> {
    @Transactional
    @Modifying
    @Query(value = "UPDATE tbl_course SET status = :status WHERE course_id = :id", nativeQuery = true)
    void updateStatusNative(@Param("status") String status, @Param("id") Integer id);

    @EntityGraph(attributePaths = {"category", "expert", "expert.role"})
    Optional<Course> findById(Integer courseId);

    @EntityGraph(attributePaths = {"category", "expert"})
    List<Course> findByStatus(String status);

    @EntityGraph(attributePaths = {"category", "expert"})
    List<Course> findByLevelTagAndStatus(String levelTag, String status);

    @EntityGraph(attributePaths = {"category", "expert"})
    List<Course> findByCategory_SettingIdAndStatus(Integer categoryId, String status);

    long countByCategory_SettingId(Integer categorySettingId);

    @Query("SELECT c FROM Course c WHERE c.courseId = :courseId")
    Optional<Course> getCourseLearningData(@Param("courseId") Integer courseId);

    // (Tùy chọn) Tìm kiếm khóa học theo tên
    @EntityGraph(attributePaths = {"category", "expert"})
    List<Course> findByTitleContainingIgnoreCaseAndStatus(String title, String status);
    
    @EntityGraph(attributePaths = {"category", "expert"})
    @Query("SELECT c FROM Course c WHERE " +
            "c.status = :status AND " +
            "(:categoryId IS NULL OR c.category.settingId = :categoryId) AND " +
            "(:keyword IS NULL OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> searchCourses(@Param("keyword") String keyword,
                               @Param("categoryId") Integer categoryId,
                               @Param("status") String status,
                               Sort sort);

    @EntityGraph(attributePaths = {"category", "expert"})
    @Query("SELECT c FROM Course c WHERE " +
            "c.status = :status AND " +
            "(:categoryId IS NULL OR c.category.settingId = :categoryId) AND " +
            "(:keyword IS NULL OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> searchCourses(@Param("keyword") String keyword,
                               @Param("categoryId") Integer categoryId,
                               @Param("status") String status,
                               Pageable pageable);

    @EntityGraph(attributePaths = {"category", "expert"})
    List<Course> findByExpertUserId(Integer expertUserId);

    @Query("SELECT c FROM Course c " +
           "LEFT JOIN c.registrations r " +
           "WHERE c.status = 'Published' AND (r IS NULL OR r.status = 'Approved') " +
           "GROUP BY c.courseId " +
           "ORDER BY COUNT(r) DESC")
    List<Course> findTopFeaturedCourses(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "expert"})
    @Query("SELECT c FROM Course c WHERE " +
            "(:expertId IS NULL OR c.expert.userId = :expertId) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:keyword IS NULL OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Course> findByExpertAndSearch(@Param("expertId") Integer expertId,
                                       @Param("keyword") String keyword,
                                       @Param("status") String status,
                                       Pageable pageable);

}



//@Repository
//public interface RegistrationRepository extends JpaRepository<Registration, Integer> {
//    // Lấy tất cả đăng ký của user (cho màn History/Cancel)
//    List<Registration> findByUser_UserIdOrderByRegistrationTimeDesc(Integer userId);
//
//    // Lấy các đăng ký đã được duyệt (cho màn My Courses)
//    @Query("SELECT r FROM Registration r WHERE r.user.userId = :userId AND r.status = 'Approved'")
//    List<Registration> findActiveRegistrations(Integer userId);
//
//    // Kiểm tra xem user đã đăng ký lớp này chưa (tránh spam)
//    boolean existsByUser_UserIdAndClazz_ClassIdAndStatusNot(Integer userId, Integer classId, String status);
//}