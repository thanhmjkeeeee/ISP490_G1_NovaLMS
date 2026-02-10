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
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/verification/**",
                                "/login/**",
                                "/oauth2/**",
                                "/index.html",
                                "/register.html", "/register",
                                "/reset-password.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/assets/**",
                                "/vendor/**",
                                "/**" // Permit static files
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // --- CẤU HÌNH FORM LOGIN (QUAN TRỌNG) ---
                .formLogin(form -> form
                        .loginPage("/login.html")          // Trang hiển thị form login
                        .loginProcessingUrl("/perform_login") // URL nhận request POST từ form (Spring tự xử lý)
                        .defaultSuccessUrl("/index.html", true) // Login thành công -> Về trang chủ
                        .failureUrl("/login.html?error=true")   // Login thất bại -> Về trang login kèm lỗi
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)

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