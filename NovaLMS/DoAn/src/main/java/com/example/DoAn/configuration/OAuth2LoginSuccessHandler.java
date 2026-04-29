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
    private final com.example.DoAn.repository.VisitorLogRepository visitorLogRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException, ServletException {
        // --- LIÊN KẾT VISITOR TOKEN (GUEST CONVERSION) ---
        if (authentication.getPrincipal() instanceof OAuth2User oauthUser) {
            String email = oauthUser.getAttribute("email");
            if (email != null) {
                userRepository.findByEmail(email).ifPresent(user -> {
                    jakarta.servlet.http.Cookie[] cookies = request.getCookies();
                    if (cookies != null) {
                        for (jakarta.servlet.http.Cookie cookie : cookies) {
                            if ("nova_visitor_token".equals(cookie.getName())) {
                                String token = cookie.getValue();
                                visitorLogRepository.findByVisitorToken(token).ifPresent(visitor -> {
                                    if (visitor.getUser() == null) {
                                        visitor.setUser(user);
                                        visitorLogRepository.save(visitor);
                                    }
                                });
                                break;
                            }
                        }
                    }
                });
            }
        }

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
