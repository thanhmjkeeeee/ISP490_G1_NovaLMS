package com.example.DoAn.service;

import com.example.DoAn.dto.UserProfileDTO;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Lấy thông tin User chuyển thành DTO
    public UserProfileDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        return UserProfileDTO.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .mobile(user.getMobile())
                .note(user.getNote())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    // Cập nhật thông tin từ DTO xuống DB
    @Transactional
    public void updateUserProfile(String email, UserProfileDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        // Chỉ cập nhật các trường được phép
        user.setFullName(dto.getFullName());
        user.setMobile(dto.getMobile());
        user.setNote(dto.getNote());

        // Không cần gọi userRepository.save(user) vì có @Transactional, Hibernate tự update
    }
    public void updateAvatar(String email, String avatarUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        user.setAvatarUrl(avatarUrl); // Tên hàm set phụ thuộc vào entity của bạn
        userRepository.save(user);
    }
}