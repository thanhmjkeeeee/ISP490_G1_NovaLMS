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

        String roleName = "ROLE_STUDENT";
        if (user.getRole() != null) {
            if (user.getRole().getName() != null && !user.getRole().getName().trim().isEmpty()) {
                roleName = user.getRole().getName();
            } else if (user.getRole().getValue() != null && !user.getRole().getValue().trim().isEmpty()) {
                roleName = user.getRole().getValue();
            }
        }

        GrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(authority))
                .disabled(!enabled)
                .build();
    }
}
