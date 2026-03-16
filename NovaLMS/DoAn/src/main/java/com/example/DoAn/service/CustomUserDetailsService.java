package com.example.DoAn.service;

import com.example.DoAn.model.Setting;
import com.example.DoAn.model.User;
import com.example.DoAn.repository.SettingRepository;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SettingRepository settingRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        boolean enabled = "Active".equalsIgnoreCase(user.getStatus());

        String roleName = "ROLE_STUDENT";

        // Query role by role_id from user
        Integer roleId = userRepository.findRoleIdByEmail(email);
        if (roleId != null) {
            Setting role = settingRepository.findById(roleId).orElse(null);
            if (role != null && role.getName() != null) {
                roleName = role.getName();
            }
        }

        log.info("Loaded user: {}, roleId: {}, roleName: {}", email, roleId, roleName);

        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(authority))
                .disabled(!enabled)
                .build();
    }
}
