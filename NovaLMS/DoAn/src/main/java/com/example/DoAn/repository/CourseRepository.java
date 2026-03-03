package com.example.DoAn.repository;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    @Query("SELECT c FROM Course c")
    Page<Course> findAllCourses(Pageable pageable);

    List<Course> findByStatus(String status);

    List<Course> findByCategory_SettingIdAndStatus(Integer categoryId, String status);

    // (Tùy chọn) Tìm kiếm khóa học theo tên
    List<Course> findByTitleContainingIgnoreCaseAndStatus(String title, String status);
    @Query("SELECT c FROM Course c WHERE " +
            "(:keyword IS NULL OR c.title LIKE %:keyword%) AND " +
            "(:categoryId IS NULL OR c.category.settingId = :categoryId) AND " +
            "c.status = :status")
    List<Course> searchCourses(@Param("keyword") String keyword,
                               @Param("categoryId") Integer categoryId,
                               @Param("status") String status,
                               Sort sort);

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