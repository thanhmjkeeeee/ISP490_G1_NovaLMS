package com.example.DoAn.service.impl;

import com.example.DoAn.dto.ProfileResponseDTO;
import com.example.DoAn.dto.ProfileRequestDTO;
import com.example.DoAn.dto.ResponseData;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.FileUploadService;
import com.example.DoAn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    @Override
    @Transactional(readOnly = true)
    public ResponseData<ProfileResponseDTO> getUserProfile(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseData.error(404, "Không tìm thấy người dùng!");
            }

            ProfileResponseDTO dto = ProfileResponseDTO.builder()
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .mobile(user.getMobile())
                    .note(user.getNote())
                    .avatarUrl(user.getAvatarUrl())
                    .build();

            return ResponseData.success("Lấy thông tin thành công", dto);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<Void> updateUserProfile(String email, ProfileRequestDTO dto) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseData.error(404, "Không tìm thấy người dùng!");
            }

            user.setFullName(dto.getFullName());
            user.setMobile(dto.getMobile());
            user.setNote(dto.getNote());

            return ResponseData.success("Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi cập nhật: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseData<String> updateAvatar(String email, MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseData.error(400, "Vui lòng chọn một file ảnh hợp lệ.");
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseData.error(404, "Không tìm thấy người dùng!");
            }

            String uploadedAvatarUrl = fileUploadService.uploadImage(file);
            user.setAvatarUrl(uploadedAvatarUrl);

            return ResponseData.success("Cập nhật ảnh đại diện thành công!", uploadedAvatarUrl);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi tải ảnh: " + e.getMessage());
        }
    }
}