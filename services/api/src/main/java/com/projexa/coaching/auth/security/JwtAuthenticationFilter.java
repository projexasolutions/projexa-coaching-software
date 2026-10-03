package com.projexa.coaching.auth.security;

import com.projexa.coaching.common.tenant.TenantContextHolder;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final JdbcTemplate db;

    public JwtAuthenticationFilter(JwtService jwt, JdbcTemplate db) {
        this.jwt = jwt;
        this.db = db;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");

        if (h != null && h.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(h.substring(7));
                UUID userId = UUID.fromString(c.getSubject());
                UUID tenantId = UUID.fromString(c.get("tenantId", String.class));

                // JWT proves identity; the database remains the source of truth for
                // current roles/permissions so permission changes take effect without
                // waiting for an old access token to expire.
                List<String> authorities = db.query(
                    """
                    select distinct authority from (
                        select 'ROLE_' || r.code as authority
                        from user_roles ur
                        join roles r on r.id = ur.role_id
                        join users u on u.id = ur.user_id
                        where ur.user_id = ?
                          and u.tenant_id = ?
                          and u.status = 'ACTIVE'
                          and (r.tenant_id is null or r.tenant_id = ?)

                        union

                        select p.code as authority
                        from user_roles ur
                        join roles r on r.id = ur.role_id
                        join role_permissions rp on rp.role_id = r.id
                        join permissions p on p.id = rp.permission_id
                        join users u on u.id = ur.user_id
                        where ur.user_id = ?
                          and u.tenant_id = ?
                          and u.status = 'ACTIVE'
                          and (r.tenant_id is null or r.tenant_id = ?)
                    ) authorities
                    order by authority
                    """,
                    ps -> {
                        ps.setObject(1, userId);
                        ps.setObject(2, tenantId);
                        ps.setObject(3, tenantId);
                        ps.setObject(4, userId);
                        ps.setObject(5, tenantId);
                        ps.setObject(6, tenantId);
                    },
                    (rs, rowNum) -> rs.getString("authority")
                );

                // Fail closed if the user no longer exists/is active or has no role.
                if (authorities.isEmpty()) {
                    SecurityContextHolder.clearContext();
                } else {
                    var granted = authorities.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(userId, null, granted)
                    );
                    TenantContextHolder.set(tenantId);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        try {
            chain.doFilter(req, res);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
