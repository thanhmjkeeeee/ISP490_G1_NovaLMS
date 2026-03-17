package com.example.DoAn.service;

import com.example.DoAn.exception.ResourceNotFoundException;
import com.example.DoAn.model.Setting;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
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
        String picture = oAuth2User.getAttribute("picture");

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // Case: User mới -> Tạo User
            Setting studentRole = settingRepository.findRoleByName("ROLE_STUDENT")
                    .orElseThrow(() -> new ResourceNotFoundException("Role"));

            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .avatarUrl(picture)
                    .role(studentRole)
                    .status("Active")
                    .authProvider("GOOGLE")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .lastLogin(LocalDateTime.now())
                    .build();

            userRepository.save(user);
        } else {
            if ("Banned".equals(user.getStatus())) {
                throw new OAuth2AuthenticationException("Account is banned");
            }

            // Cập nhật thông tin mới nhất từ Google
            user.setFullName(name);
            user.setAvatarUrl(picture);
            user.setLastLogin(LocalDateTime.now());
            user.setAuthProvider("GOOGLE");
            userRepository.save(user);
        }

        // Lấy role từ database và tạo authorities
        String roleName = "ROLE_STUDENT";
        if (user.getRole() != null && user.getRole().getName() != null) {
            roleName = user.getRole().getName();
        }

        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        // Trả về OAuth2User với authorities từ database
        return new DefaultOAuth2User(
                Collections.singletonList(authority),
                oAuth2User.getAttributes(),
                "email"
        );
    }
}