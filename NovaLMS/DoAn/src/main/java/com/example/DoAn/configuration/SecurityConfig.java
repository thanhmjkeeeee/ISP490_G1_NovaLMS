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
            "/placement-test", "/placement-test/**",
            "/placement-test/results/**",
            // Hybrid / Guest placement test
            "/hybrid-entry", "/hybrid-entry/**",
            "/hybrid/**",
            "/courses", "/courses.html",
            "/course-details", "/course-details.html",
            "/course/details", "/course/details/**",
            "/instructors", "/instructors.html", "/instructor-profile", "/instructor-profile.html",
            "/about", "/about.html", "/pricing", "/pricing.html",
            "/blog", "/blog.html", "/contact", "/contact.html",
            "/404", "/404.html", "/error", "/classes",
            "/enroll", "/enroll-class",
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
                                response.getWriter().write("{\"status\":401,\"message\":\"Unauthorized\"}");
                            } else {
                                response.sendRedirect("/login.html?redirect=" + request.getRequestURI());
                            }
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(STATIC_RESOURCES).permitAll()
                        .requestMatchers(PUBLIC_PAGES).permitAll()
                        .requestMatchers("/login", "/login.html",
                                "/register", "/register.html",
                                "/reset-password", "/reset-password.html",
                                "/error").permitAll() // Thêm /error
                        .requestMatchers("/api/v1/auth/current-user").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/classes/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/payment/webhook").permitAll()
                        .requestMatchers("/api/**").authenticated()

                        // Phân quyền cứng
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        .requestMatchers("/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/expert/**").hasRole("EXPERT")
                        .requestMatchers("/student/**").hasRole("STUDENT")

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

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
            String targetUrl = redirectUrl != null ? redirectUrl : "/index";

            // Nếu không có redirect, chuyển theo role
            if (redirectUrl == null) {
                var authorities = authentication.getAuthorities();
                for (var authority : authorities) {
                    String role = authority.getAuthority();
                    if (role.equals("ROLE_ADMIN")) {
                        targetUrl = "/admin/dashboard"; break;
                    } else if (role.equals("ROLE_MANAGER")) {
                        targetUrl = "/manager/dashboard"; break;
                    } else if (role.equals("ROLE_TEACHER")) {
                        targetUrl = "/teacher/dashboard"; break;
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