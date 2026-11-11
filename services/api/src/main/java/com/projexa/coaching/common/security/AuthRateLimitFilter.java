package com.projexa.coaching.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Shared IP throttling for public authentication endpoints.
 * Redis stores counters across API instances. If the limiter is unavailable,
 * authentication endpoints fail closed rather than silently losing protection.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
        "local count = redis.call('INCR', KEYS[1]); " +
        "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; " +
        "return count",
        Long.class
    );

    private final StringRedisTemplate redis;

    public AuthRateLimitFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int limit = limit(request.getMethod(), request.getRequestURI());
        if (limit == 0) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String key = "projexa:auth-rate:" + digest(request.getRemoteAddr()) + ":" + request.getRequestURI();
            Long count = redis.execute(INCREMENT_SCRIPT, List.of(key), Long.toString(WINDOW_MS));
            if (count == null) {
                unavailable(response);
                return;
            }
            if (count > limit) {
                reject(response);
                return;
            }
        } catch (RuntimeException ex) {
            unavailable(response);
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

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to fingerprint client address", ex);
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "900");
        response.getWriter().write("{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please try again later.\"}}");
    }

    private void unavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"error\":{\"code\":\"AUTH_PROTECTION_UNAVAILABLE\",\"message\":\"Authentication is temporarily unavailable. Please try again shortly.\"}}");
    }
}
