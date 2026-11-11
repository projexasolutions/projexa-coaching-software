package com.projexa.coaching.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-process IP throttling for public authentication endpoints.
 * Deployments with multiple API instances should additionally enforce a shared
 * edge/Redis rate limit; this filter is a local defense-in-depth layer.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final int MAX_BUCKETS = 20_000;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int limit = limit(request.getMethod(), request.getRequestURI());
        if (limit == 0) {
            chain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        if (buckets.size() >= MAX_BUCKETS) {
            buckets.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MS);
        }
        if (buckets.size() >= MAX_BUCKETS) {
            reject(response);
            return;
        }

        Bucket bucket = buckets.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt >= WINDOW_MS) return new Bucket(now, 1);
            current.count++;
            return current;
        });
        if (bucket.count > limit) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private int limit(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) return 0;
        return switch (path) {
            case "/api/v1/auth/login" -> 20;
            case "/api/v1/auth/signup" -> 5;
            case "/api/v1/auth/verify-institute" -> 30;
            case "/api/v1/auth/password-reset/request" -> 5;
            case "/api/v1/auth/password-reset/confirm" -> 10;
            case "/api/v1/invitations/accept" -> 10;
            default -> 0;
        };
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "900");
        response.getWriter().write("{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please try again later.\"}}");
    }

    private static final class Bucket {
        private final long startedAt;
        private int count;
        private Bucket(long startedAt, int count) { this.startedAt = startedAt; this.count = count; }
    }
}
