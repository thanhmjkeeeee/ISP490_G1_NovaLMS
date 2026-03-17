package com.example.DoAn.configuration;

import com.example.DoAn.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String targetUrl = "/index";

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                targetUrl = "/admin/dashboard";
                break;
            } else if (role.equals("ROLE_MANAGER")) {
                targetUrl = "/manager/dashboard";
                break;
            } else if (role.equals("ROLE_TEACHER")) {
                targetUrl = "/teacher/dashboard";
                break;
            } else if (role.equals("ROLE_STUDENT")) {
                targetUrl = "/student/dashboard";
                break;
            } else if (role.equals("ROLE_EXPERT")) {
                targetUrl = "/expert/dashboard";
                break;
            }
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
