package com.example.DoAn.service;

import com.example.DoAn.dto.ServiceResult;
import com.example.DoAn.dto.UserProfileDTO;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    @Transactional(readOnly = true)
    public ServiceResult<UserProfileDTO> getUserProfile(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ServiceResult.failure("Không tìm thấy người dùng!");
        }

        UserProfileDTO dto = UserProfileDTO.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .mobile(user.getMobile())
                .note(user.getNote())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return ServiceResult.success("Lấy thông tin thành công", dto);
    }

    @Transactional
    public ServiceResult<Void> updateUserProfile(String email, UserProfileDTO dto) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                return ServiceResult.failure("Không tìm thấy người dùng!");
            }

            user.setFullName(dto.getFullName());
            user.setMobile(dto.getMobile());
            user.setNote(dto.getNote());

            return ServiceResult.success("Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi cập nhật: " + e.getMessage());
        }
    }

    @Transactional
    public ServiceResult<String> updateAvatar(String email, MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ServiceResult.failure("Vui lòng chọn một file ảnh hợp lệ.");
            }

            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                return ServiceResult.failure("Không tìm thấy người dùng!");
            }

            String uploadedAvatarUrl = fileUploadService.uploadImage(file);
            user.setAvatarUrl(uploadedAvatarUrl);

            return ServiceResult.success("Cập nhật ảnh đại diện thành công!", uploadedAvatarUrl);
        } catch (Exception e) {
            return ServiceResult.failure("Lỗi tải ảnh: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAllUsersWithRole(pageable);
    }
}