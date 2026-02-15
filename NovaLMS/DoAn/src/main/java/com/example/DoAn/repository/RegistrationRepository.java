package com.example.DoAn.repository;

import com.example.DoAn.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Integer> {

    // 1. Lấy lịch sử đăng ký của user (Mới nhất lên đầu) - Dùng cho màn My Enrollments
    List<Registration> findByUser_UserIdOrderByRegistrationTimeDesc(Integer userId);

    // 2. Lấy các khóa học đã được duyệt (Active/Approved) - Dùng cho màn My Courses
    @Query("SELECT r FROM Registration r WHERE r.user.userId = :userId AND r.status = 'Approved'")
    List<Registration> findActiveRegistrations(Integer userId);

    // 3. Kiểm tra xem user đã đăng ký lớp này chưa (tránh trùng lặp)
    // Logic: Tìm xem có record nào của user này, lớp này mà trạng thái KHÔNG PHẢI là 'Cancelled'
    boolean existsByUser_UserIdAndClazz_ClassIdAndStatusNot(Integer userId, Integer classId, String status);
}