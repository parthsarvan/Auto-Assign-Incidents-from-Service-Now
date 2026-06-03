package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class ApiRequestSizeLimitFilter extends OncePerRequestFilter {
    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");

    @Value("${inciteam.security.max-api-request-bytes:1048576}")
    private long maxApiRequestBytes;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (BODY_METHODS.contains(request.getMethod()) && contentLength > maxApiRequestBytes) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType("text/plain;charset=UTF-8");
            response.getOutputStream().write("Request body is too large.".getBytes(StandardCharsets.UTF_8));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
