package com.example.DoAn.service;

import com.example.DoAn.model.Setting;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture"); // Lấy avatar nếu cần

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Case: User mới -> Tạo User

            // --- SỬA LỖI TẠI ĐÂY: Đổi "Student" thành "ROLE_STUDENT" ---
            Setting studentRole = settingRepository.findRoleByValue("ROLE_STUDENT")
                    .orElseThrow(() -> new RuntimeException("Default Role 'ROLE_STUDENT' not found. Hãy chạy DataInitializer hoặc insert vào DB."));

            // SQL bắt buộc password NOT NULL -> tạo dummy password
            user = User.builder()
                    .email(email)
                    .fullName(name) // Map thêm tên từ Google
                    .avatarUrl(picture) // Map thêm avatar
                    .role(studentRole)
                    .status("Active")
                    .authProvider("GOOGLE") // Đánh dấu là Google User
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .lastLogin(LocalDateTime.now())
                    .build();

            userRepository.save(user);
        } else {
            // Nếu user đã tồn tại, kiểm tra xem có bị khóa không
            if ("Banned".equals(user.getStatus())) {
                throw new OAuth2AuthenticationException("Account is banned");
            }

            // Cập nhật thông tin mới nhất từ Google (Optional)
            user.setFullName(name);
            user.setAvatarUrl(picture);
            user.setLastLogin(LocalDateTime.now());
            user.setAuthProvider("GOOGLE"); // Link account nếu trước đó là LOCAL
            userRepository.save(user);
        }

        return oAuth2User;
    }
}