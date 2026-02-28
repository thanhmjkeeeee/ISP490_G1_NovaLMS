package com.example.DoAn.configuration;

import com.example.DoAn.service.CustomOAuth2UserService;
import com.example.DoAn.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CachingUserDetailsService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// Lưu ý: Đảm bảo bạn đã import OAuth2LoginSuccessHandler nếu class đó tồn tại
// import com.example.DoAn.configuration.OAuth2LoginSuccessHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    // Đảm bảo class này đã được tạo và có annotation @Component hoặc @Service
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. Cho phép tài nguyên tĩnh (Bắt buộc phải tách rõ)
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/assets/**", "/vendor/**", "/favicon.ico"
                        ).permitAll()

                        // 2. Cho phép các API xác thực và các trang công khai
                        .requestMatchers(
                                "/", "/index", "/index.html",
                                "/login", "/login.html",
                                "/register", "/register.html",
                                "/api/auth/**", "/api/verification/**",
                                "/reset-password", "/reset-password.html",
                                "/courses", "/courses.html",
                                "/course-details", "/course-details.html",
                                "/instructors", "/instructors.html",
                                "/instructor-profile", "/instructor-profile.html",
                                "/about", "/about.html",
                                "/pricing", "/pricing.html",
                                "/blog", "/blog.html",
                                "/contact", "/contact.html",
                                "/404", "/404.html",
                                "/error"
                        ).permitAll()

                        // 3. Bảo vệ các luồng nghiệp vụ của Student (Quan trọng)
                        .requestMatchers("/student/**").authenticated()
                        .requestMatchers("/profile/**").authenticated()

                        // 4. Các yêu cầu còn lại phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/perform_login")
                        .defaultSuccessUrl("/index.html", true)
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            response.sendRedirect("/student/my-enrollments");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/index.html")
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

}