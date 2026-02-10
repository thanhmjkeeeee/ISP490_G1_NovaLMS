package com.example.DoAn.service;

import com.example.DoAn.model.User;
import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        boolean enabled = "Active".equalsIgnoreCase(user.getStatus());

        // Lấy role từ bảng Setting. Field 'value' chứa "ROLE_STUDENT", "ROLE_ADMIN"...
        String roleName = (user.getRole() != null) ? user.getRole().getValue() : "ROLE_STUDENT";
        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // DB mới dùng field 'password'
                .authorities(Collections.singletonList(authority))
                .disabled(!enabled)
                .build();
    }
}
