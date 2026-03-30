package com.example.DoAn.service.impl;

import com.example.DoAn.dto.request.ChangePasswordRequest;
import com.example.DoAn.dto.response.ProfileResponseDTO;
import com.example.DoAn.dto.request.ProfileRequestDTO;
import com.example.DoAn.dto.response.ResponseData;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.UserRepository;
import com.example.DoAn.service.FileUploadService;
import com.example.DoAn.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final PasswordEncoder passwordEncoder;

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
            if (dto.getMobile() != null && !dto.getMobile().trim().isEmpty()) {
                String phoneRegex = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$";
                if (!dto.getMobile().matches(phoneRegex)) {
                    return ResponseData.error(400, "Số điện thoại không hợp lệ (Phải là số Việt Nam có 10 chữ số)!");
                }
            }

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

            String uploadedAvatarUrl = fileUploadService.uploadFile(file, "avatar");
            user.setAvatarUrl(uploadedAvatarUrl);

            return ResponseData.success("Cập nhật ảnh đại diện thành công!", uploadedAvatarUrl);
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi tải ảnh: " + e.getMessage());
        }
    }
    @Override
    @Transactional
    public ResponseData<Void> changePassword(String email, ChangePasswordRequest request) {
        try {
            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                return ResponseData.error(400, "Mật khẩu mới không được để trống!");
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseData.error(400, "Mật khẩu xác nhận không khớp!");
            }

            String pwdRegex = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+-=<>?]).{6,}$";
            if (!request.getNewPassword().matches(pwdRegex)) {
                return ResponseData.error(400, "Mật khẩu mới không đạt yêu cầu bảo mật (Ít nhất 6 kí tự, 1 viết hoa, 1 kí tự đặc biệt)!");
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseData.error(404, "Không tìm thấy người dùng!");
            }

            // Kiểm tra mật khẩu cũ
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                return ResponseData.error(400, "Mật khẩu hiện tại không chính xác!");
            }

            // Mật khẩu mới không được trùng mật khẩu cũ
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return ResponseData.error(400, "Mật khẩu mới không được trùng với mật khẩu hiện tại!");
            }

            // Hash và lưu
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return ResponseData.success("Đổi mật khẩu thành công!");
        } catch (Exception e) {
            return ResponseData.error(500, "Lỗi hệ thống: " + e.getMessage());
        }
    }
}