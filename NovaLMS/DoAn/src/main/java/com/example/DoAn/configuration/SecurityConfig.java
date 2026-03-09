package com.example.DoAn.configuration;

import com.example.DoAn.service.CustomOAuth2UserService;
import com.example.DoAn.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final CustomUserDetailsService customUserDetailsService;

    private static final String[] STATIC_RESOURCES = {
            "/css/**", "/js/**", "/images/**", "/assets/**", "/vendor/**", "/favicon.ico"
    };

    private static final String[] PUBLIC_PAGES = {
            "/", "/index", "/index.html",
            "/courses", "/courses.html",
            "/course-details", "/course-details.html",
            "/instructors", "/instructors.html", "/instructor-profile", "/instructor-profile.html",
            "/about", "/about.html",
            "/pricing", "/pricing.html",
            "/blog", "/blog.html",
            "/contact", "/contact.html",
            "/404", "/404.html", "/error", "/classes"
    };

    private static final String[] AUTH_PAGES = {
            "/login", "/login.html",
            "/register", "/register.html",
            "/reset-password", "/reset-password.html",
            "/api/auth/**", "/api/verification/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(STATIC_RESOURCES).permitAll()
                        .requestMatchers(PUBLIC_PAGES).permitAll()
                        .requestMatchers(AUTH_PAGES).permitAll()
                        .requestMatchers("/api/v1/classes/**").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/api/v1/password/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/learning/**", "/api/v1/profile/**", "/api/v1/student/**").authenticated()
                        .requestMatchers("/student/**", "/profile/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasRole("MANAGER")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/perform_login")
                        .successHandler(roleBasedSuccessHandler())
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login.html")
                        .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }


    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();

                if (role.equals("ROLE_ADMIN")) {
                    response.sendRedirect("/admin/dashboard");
                    return;
                } else if (role.equals("ROLE_MANAGER")) {
                    response.sendRedirect("/manager/dashboard");
                    return;
                } else if (role.equals("ROLE_TEACHER")) {
                    response.sendRedirect("/teacher/dashboard");
                    return;
                } else if (role.equals("ROLE_EXPERT")) {
                    response.sendRedirect("/expert/dashboard");
                    return;
                } else if (role.equals("ROLE_STUDENT") || role.equals("ROLE_USER")) {
                    response.sendRedirect("/student/dashboard");
                    return;
                }
            }

            response.sendRedirect("/");
        };
    }
}