package com.example.DoAn.repository;

import com.example.DoAn.model.Clazz;
import com.example.DoAn.model.Course;
import com.example.DoAn.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    List<Course> findByStatus(String status);
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