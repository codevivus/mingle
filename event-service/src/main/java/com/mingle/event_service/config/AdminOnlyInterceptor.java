package com.mingle.event_service.config;

import com.mingle.event_service.exception.type.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Event create/update/delete/import are admin only; reads stay open. The gateway sets
 * X-User-Role from the authenticated user, so the header is trusted here.
 */
@Component
public class AdminOnlyInterceptor implements HandlerInterceptor {

    private static final String ROLE_HEADER = "X-User-Role";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (READ_METHODS.contains(request.getMethod())) {
            return true;
        }
        if (!"ADMIN".equals(request.getHeader(ROLE_HEADER))) {
            throw new ForbiddenException("Admin role required");
        }
        return true;
    }
}
