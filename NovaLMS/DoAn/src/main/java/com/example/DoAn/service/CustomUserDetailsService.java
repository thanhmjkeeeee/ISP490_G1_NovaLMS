package com.example.DoAn.service;

import com.example.DoAn.model.User;
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + email));

        boolean enabled = "Active".equalsIgnoreCase(user.getStatus());

        String roleName = "ROLE_STUDENT";

        // Get role from user association
        if (user.getRole() != null && user.getRole().getValue() != null) {
            String val = user.getRole().getValue();
            roleName = val.startsWith("ROLE_") ? val : "ROLE_" + val;
        }

        log.info("Loaded user: {}, roleName: {}", email, roleName);

        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(authority))
                .disabled(!enabled)
                .build();
    }
}
