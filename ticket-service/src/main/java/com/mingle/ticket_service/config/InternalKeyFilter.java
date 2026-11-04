package com.mingle.ticket_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /internal endpoints are for other services only. The gateway does not route them, but anything on the
 * Docker network can reach this port, so they also need the shared INTERNAL_API_KEY.
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    static final int MIN_KEY_LENGTH = 16;

    private final byte[] expected;

    public InternalKeyFilter(@Value("${internal.api-key:}") String key) {
        if (key.length() < MIN_KEY_LENGTH) {
            throw new IllegalStateException("INTERNAL_API_KEY must be at least " + MIN_KEY_LENGTH + " characters");
        }
        this.expected = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader("X-Internal-Key");
        if (provided == null || !MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
