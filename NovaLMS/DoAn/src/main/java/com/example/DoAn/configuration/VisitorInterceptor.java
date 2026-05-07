package com.example.DoAn.configuration;

import com.example.DoAn.model.VisitorLog;
import com.example.DoAn.repository.VisitorLogRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VisitorInterceptor implements HandlerInterceptor {

    private final VisitorLogRepository visitorLogRepository;
    private static final String VISITOR_COOKIE_NAME = "nova_visitor_token";

    /** Chỉ track các public page — không track admin/teacher/student/api */
    private boolean isPublicPage(String path) {
        return path.startsWith("/courses") ||
               path.startsWith("/course/") ||
               path.equals("/") ||
               path.startsWith("/home") ||
               path.startsWith("/about") ||
               path.startsWith("/contact") ||
               path.startsWith("/login") ||
               path.startsWith("/register") ||
               path.startsWith("/public");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // Bỏ qua static resources, API calls
        if (path.contains(".") || path.startsWith("/api/") || path.startsWith("/ws/")) {
            return true;
        }

        // Chỉ track public pages (không track /admin, /teacher, /student, /expert)
        if (!isPublicPage(path)) {
            return true;
        }

        // Bỏ qua nếu user đã đăng nhập (có authentication)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        String visitorToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (VISITOR_COOKIE_NAME.equals(cookie.getName())) {
                    visitorToken = cookie.getValue();
                    break;
                }
            }
        }

        if (visitorToken == null) {
            // New anonymous visitor on a public page
            visitorToken = UUID.randomUUID().toString();
            Cookie visitorCookie = new Cookie(VISITOR_COOKIE_NAME, visitorToken);
            visitorCookie.setPath("/");
            visitorCookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
            visitorCookie.setHttpOnly(true);
            response.addCookie(visitorCookie);

            VisitorLog visitorLog = VisitorLog.builder()
                    .visitorToken(visitorToken)
                    .ipAddress(request.getRemoteAddr())
                    .userAgent(request.getHeader("User-Agent"))
                    .build();
            visitorLogRepository.save(visitorLog);
            log.info("New guest visitor detected. Token: {}", visitorToken);
        } else {
            // Existing visitor — update last_visit
            final String token = visitorToken;
            visitorLogRepository.findByVisitorToken(token).ifPresent(logEntry -> {
                logEntry.setLastVisit(LocalDateTime.now());
                visitorLogRepository.save(logEntry);
            });
        }

        return true;
    }
}
