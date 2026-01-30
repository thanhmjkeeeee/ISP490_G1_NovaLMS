package com.example.DoAn.service;

import com.example.DoAn.model.Role;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.RoleRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");

        // Kiểm tra xem user đã tồn tại chưa
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Case: User mới -> Tạo User

            // --- SỬA LỖI TẠI ĐÂY: Đổi "Student" thành "ROLE_STUDENT" ---
            Role studentRole = roleRepository.findByName("ROLE_STUDENT")
                    .orElseThrow(() -> new RuntimeException("Default Role 'ROLE_STUDENT' not found. Hãy chạy DataInitializer hoặc insert vào DB."));

            // SQL bắt buộc password_hash NOT NULL -> tạo dummy password
            user = User.builder()
                    .email(email)
                    .role(studentRole)
                    .status("Active")
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .build();

            userRepository.save(user);
        } else {
            // Nếu user đã tồn tại, kiểm tra xem có bị khóa không
            if ("Banned".equals(user.getStatus())) { // Giả định getter là getStatus() hoặc public field
                throw new OAuth2AuthenticationException("Account is banned");
            }
        }

        return oAuth2User;
    }
}