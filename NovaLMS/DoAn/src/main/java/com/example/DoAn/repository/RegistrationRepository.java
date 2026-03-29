package com.example.DoAn.repository;

import com.example.DoAn.model.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Integer> {

    // Lấy lịch sử đăng ký của user (Mới nhất lên đầu) - Dùng cho màn My Enrollments
    @Query("SELECT r FROM Registration r WHERE r.user.userId = :userId ORDER BY r.registrationTime DESC")
    List<Registration> findByUser_UserIdOrderByRegistrationTimeDesc(@Param("userId") Integer userId);

    @Query("SELECT r FROM Registration r WHERE r.user.email = :email ORDER BY r.registrationTime DESC")
    List<Registration> findByUserEmail(@Param("email") String email);

    Optional<Registration> findByUser_EmailAndClazz_ClassIdAndStatus(String email, Integer classId, String status);
    // Kiểm tra xem User đã đăng ký lớp học này với trạng thái khác status truyền vào chưa
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Registration r WHERE r.user.userId = :userId AND r.clazz.classId = :classId AND r.status <> :status")
    boolean existsByUser_UserIdAndClazz_ClassIdAndStatusNot(
            @Param("userId") Integer userId,
            @Param("classId") Integer classId,
            @Param("status") String status
    );

    // Lấy bản ghi đăng ký đầu tiên theo User, Course và Status
    @Query("SELECT r FROM Registration r WHERE r.user.userId = :userId AND r.course.courseId = :courseId AND r.status = :status")
    Optional<Registration> findFirstByUserIdAndCourseIdAndStatus(
            @Param("userId") Integer userId,
            @Param("courseId") Integer courseId,
            @Param("status") String status
    );

    @Query(value = """
    SELECT r FROM Registration r 
    JOIN FETCH r.course c
    JOIN FETCH r.clazz cl
    LEFT JOIN FETCH cl.teacher t
    WHERE r.user.userId = :userId 
      AND r.status = 'Approved'
      AND (:keyword IS NULL OR LOWER(cl.className) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:status IS NULL OR cl.status = :status)
""", countQuery = """
    SELECT COUNT(r) FROM Registration r 
    JOIN r.course c
    JOIN r.clazz cl
    WHERE r.user.userId = :userId 
      AND r.status = 'Approved'
      AND (:keyword IS NULL OR LOWER(cl.className) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:status IS NULL OR cl.status = :status)
""")
    Page<Registration> findMyClassesWithFilters(
            @Param("userId") Integer userId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    @Query(value = """
        SELECT r FROM Registration r 
        JOIN FETCH r.course c
        JOIN FETCH r.clazz cl
        LEFT JOIN FETCH c.category cat
        WHERE r.user.userId = :userId 
          AND r.status = 'Approved'
          AND (:keyword IS NULL OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR cat.settingId = :categoryId)
    """, countQuery = """
        SELECT COUNT(r) FROM Registration r 
        JOIN r.course c
        LEFT JOIN c.category cat
        WHERE r.user.userId = :userId 
          AND r.status = 'Approved'
          AND (:keyword IS NULL OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR cat.settingId = :categoryId)
    """)
    Page<Registration> findMyCoursesWithFilters(
            @Param("userId") Integer userId,
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );

    // Đếm số lượng học viên đã đăng ký 1 khóa học
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.course.courseId = :courseId AND r.status = :status")
    long countByCourse_CourseIdAndStatus(
            @Param("courseId") Integer courseId,
            @Param("status") String status
    );

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.course.courseId = :courseId")
    long countByCourse_CourseId(@Param("courseId") Integer courseId);

    // Tìm đăng ký đang chờ (PENDING/Submitted) — cho phép retry thanh toán khi user back trình duyệt
    @Query("SELECT r FROM Registration r WHERE r.user.userId = :userId AND r.clazz.classId = :classId AND r.status IN :statuses")
    Optional<Registration> findByUser_UserIdAndClazz_ClassIdAndStatusIn(
            @Param("userId") Integer userId,
            @Param("classId") Integer classId,
            @Param("statuses") List<String> statuses
    );

    // Kiểm tra xem User đã đăng ký KHÓA HỌC này chưa (không chỉ là Lớp - Clazz)
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Registration r WHERE r.user.userId = :userId AND r.course.courseId = :courseId AND r.status = :status")
    boolean existsByUser_UserIdAndCourse_CourseIdAndStatus(
            @Param("userId") Integer userId,
            @Param("courseId") Integer courseId,
            @Param("status") String status
    );

    // Lấy danh sách đăng ký theo Khóa học (Dùng cho Admin/Manager quản lý)
    @Query("SELECT r FROM Registration r WHERE r.course.courseId = :courseId")
    List<Registration> findByCourse_CourseId(@Param("courseId") Integer courseId);
    Optional<Registration> findByUser_UserIdAndCourse_CourseIdAndStatus(Integer userId, Integer courseId, String status);

    // Admin: Lấy tất cả đăng ký
    @Query("SELECT r FROM Registration r JOIN FETCH r.user u JOIN FETCH r.course c JOIN FETCH r.clazz cl ORDER BY r.registrationTime DESC")
    List<Registration> findAllRegistrations();

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.course.courseId = :courseId OR :courseId IS NULL")
    long countAll(@Param("courseId") Integer courseId);

    List<Registration> findTop10ByOrderByRegistrationTimeDesc();

    long countByRegistrationTimeAfter(java.time.LocalDateTime date);

    // Kiểm tra enrollment theo class (dùng cho quiz gắn với class)
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Registration r WHERE r.user.userId = :userId AND r.clazz.classId = :classId AND r.status = 'Approved'")
    boolean existsByUser_UserIdAndClazz_ClassIdAndStatusApproved(
            @Param("userId") Integer userId,
            @Param("classId") Integer classId
    );

    @Query("SELECT r FROM Registration r JOIN FETCH r.user u WHERE r.clazz.classId = :classId AND r.status = 'Approved' ORDER BY u.fullName ASC")
    List<Registration> findApprovedByClassId(@Param("classId") Integer classId);



}