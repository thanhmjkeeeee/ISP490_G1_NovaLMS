package com.example.DoAn.configuration;

import com.example.DoAn.service.CustomOAuth2UserService;
import com.example.DoAn.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletResponse;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    private static final String[] STATIC_RESOURCES = {
            "/css/**", "/js/**", "/images/**", "/assets/**", "/vendor/**", "/favicon.ico"
    };

    private static final String[] PUBLIC_PAGES = {
            "/", "/index", "/index.html",
            "/courses", "/courses.html",
            "/course-details", "/course-details.html",
            "/course/details", "/course/details/**",
            "/instructors", "/instructors.html", "/instructor-profile", "/instructor-profile.html",
            "/about", "/about.html", "/pricing", "/pricing.html",
            "/blog", "/blog.html", "/contact", "/contact.html",
            "/404", "/404.html", "/403", "/403.html", "/error", "/classes",
            "/enroll", "/enroll/**", "/enroll-class",
            // PayOS payment return URLs
            "/payment/success", "/payment/cancel"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // Cần thiết cho Thymeleaf
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"status\":401,\"message\":\"Vui lòng đăng nhập.\"}");
                            } else {
                                response.sendRedirect("/login.html?redirect=" + request.getRequestURI());
                            }
                        })
                        .accessDeniedPage("/403.html")
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(Arrays.stream(STATIC_RESOURCES).map(AntPathRequestMatcher::antMatcher).toArray(AntPathRequestMatcher[]::new)).permitAll()
                        .requestMatchers(Arrays.stream(PUBLIC_PAGES).map(AntPathRequestMatcher::antMatcher).toArray(AntPathRequestMatcher[]::new)).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/login"), AntPathRequestMatcher.antMatcher("/login.html"),
                                AntPathRequestMatcher.antMatcher("/register"), AntPathRequestMatcher.antMatcher("/register.html"),
                                AntPathRequestMatcher.antMatcher("/reset-password"), AntPathRequestMatcher.antMatcher("/reset-password.html"),
                                AntPathRequestMatcher.antMatcher("/403.html"), AntPathRequestMatcher.antMatcher("/error")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/auth/current-user")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/auth/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/classes/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/public/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/files/preview")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/payment/webhook")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/admin/debug/**")).permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/**")).authenticated()

                        // Phân quyền cứng
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/admin/registrations/**")).hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/admin/**")).hasRole("ADMIN")
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/manager/**")).hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/teacher/**")).hasRole("TEACHER")
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/expert/**")).hasAnyRole("EXPERT", "ADMIN")
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/expert/assignments/**")).hasAnyAuthority("ROLE_EXPERT", "ROLE_ADMIN")
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/student/**")).hasRole("STUDENT")

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/perform_login")
                        .successHandler(customSuccessHandler())
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login.html")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login.html?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Cache-Control",
                "x-payos-signature", "x-client-id", "x-api-key"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            // Kiểm tra có redirect parameter không
            String redirectUrl = request.getParameter("redirect");
            String targetUrl = (redirectUrl != null && !redirectUrl.isEmpty()) ? redirectUrl : "/index";

            // Nếu không có redirect hoặc redirect về trang chủ, chuyển theo role
            if (redirectUrl == null || redirectUrl.isEmpty() || redirectUrl.equals("/") || redirectUrl.equals("/index") || redirectUrl.equals("/index.html")) {
                var authorities = authentication.getAuthorities();
                for (var authority : authorities) {
                    String role = authority.getAuthority();
                    if (role.equals("ROLE_ADMIN")) {
                        targetUrl = "/admin/dashboard"; break;
                    } else if (role.equals("ROLE_MANAGER")) {
                        targetUrl = "/manager/dashboard"; break;
                    } else if (role.equals("ROLE_TEACHER")) {
                        targetUrl = "/teacher/dashboard"; break;
                    } else if (role.equals("ROLE_EXPERT")) {
                        targetUrl = "/expert/dashboard"; break;
                    } else if (role.equals("ROLE_STUDENT")) {
                        targetUrl = "/student/dashboard"; break;
                    }
                }
            }
            response.sendRedirect(targetUrl);
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}