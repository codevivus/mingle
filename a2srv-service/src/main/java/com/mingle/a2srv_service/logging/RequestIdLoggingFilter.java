package com.mingle.a2srv_service.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class RequestIdLoggingFilter implements Filter {

    private static final String HEADER_NAME = "X-Request-ID";
    private static final String MDC_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        try {
            String requestId = Optional.ofNullable(request.getHeader(HEADER_NAME))
                    .filter(s -> !s.isBlank())
                    .orElse(UUID.randomUUID().toString());

            MDC.put(MDC_KEY, requestId);
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
