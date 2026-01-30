package com.example.DoAn.configuration;

import com.example.DoAn.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/login/**",
                                "/oauth2/**",
                                "/index.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/**" // Permit static files
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                );
        return http.build();
    }

    // ĐÃ XÓA BEAN passwordEncoder() Ở ĐÂY ĐỂ TRÁNH LỖI CIRCULAR DEPENDENCY
}