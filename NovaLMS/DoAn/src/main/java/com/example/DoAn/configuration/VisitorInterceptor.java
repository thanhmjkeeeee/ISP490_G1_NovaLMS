package com.example.DoAn.configuration;

import com.example.DoAn.model.VisitorLog;
import com.example.DoAn.repository.VisitorLogRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip static resources and API calls if needed, but for tracking we usually want all page hits
        String path = request.getRequestURI();
        if (path.contains(".") || path.startsWith("/api/")) {
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
            // New Visitor
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
            // Existing Visitor
            final String token = visitorToken;
            visitorLogRepository.findByVisitorToken(token).ifPresent(logEntry -> {
                logEntry.setLastVisit(LocalDateTime.now());
                visitorLogRepository.save(logEntry);
            });
        }

        return true;
    }
}
