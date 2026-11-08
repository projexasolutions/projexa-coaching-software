package com.projexa.coaching.auth.service;

import com.projexa.coaching.auth.dto.*;
import com.projexa.coaching.auth.security.JwtService;
import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.tenancy.entity.Tenant;
import com.projexa.coaching.tenancy.repository.TenantRepository;
import com.projexa.coaching.users.entity.*;
import com.projexa.coaching.users.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final TenantRepository tenants;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final SecureRandom random = new SecureRandom();

    public AuthService(TenantRepository tenants, UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwt) {
        this.tenants = tenants;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        Tenant tenant = tenants.findBySlugAndStatus(req.tenantSlug().toLowerCase(Locale.ROOT), "ACTIVE")
            .orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Invalid institute or credentials"));
        User user = users.findByTenantIdAndEmail(tenant.getId(), req.email().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ApiException("INVALID_CREDENTIALS", "Invalid institute or credentials"));
        if (!"ACTIVE".equals(user.getStatus()) || !encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException("INVALID_CREDENTIALS", "Invalid institute or credentials");
        }
        return issue(user, tenant);
    }

    @Transactional
    public LoginResponse refresh(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
        }
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = refreshTokens.findByTokenHashAndRevokedAtIsNull(hash(raw))
            .filter(candidate -> candidate.getExpiresAt().isAfter(now))
            .orElseThrow(() -> new ApiException("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired"));

        User user = users.findById(token.getUserId())
            .orElseThrow(() -> new ApiException("INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired"));
        if (!"ACTIVE".equals(user.getStatus())) {
            token.setRevokedAt(now);
            token.setLastUsedAt(now);
            throw new ApiException("USER_INACTIVE", "User account is inactive");
        }

        Tenant tenant = tenants.findByIdAndStatus(user.getTenantId(), "ACTIVE")
            .orElseThrow(() -> new ApiException("TENANT_INACTIVE", "Institute is inactive"));

        // Rotate refresh tokens: the old token is invalidated before a new one is issued.
        token.setRevokedAt(now);
        token.setLastUsedAt(now);
        return issue(user, tenant);
    }

    @Transactional
    public void logout(String raw) {
        if (raw == null || raw.isBlank()) return;
        refreshTokens.findByTokenHashAndRevokedAtIsNull(hash(raw)).ifPresent(token -> {
            LocalDateTime now = LocalDateTime.now();
            token.setRevokedAt(now);
            token.setLastUsedAt(now);
        });
    }

    private LoginResponse issue(User user, Tenant tenant) {
        Set<String> roles = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(Permission::getCode).collect(Collectors.toSet());
        Set<String> authorities = new HashSet<>(permissions);
        roles.forEach(role -> authorities.add("ROLE_" + role));

        String access = jwt.generate(user.getId(), tenant.getId(), user.getEmail(), authorities);
        String refresh = generateRefresh();
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(refresh));
        token.setExpiresAt(LocalDateTime.now().plusDays(30));
        refreshTokens.save(token);

        return new LoginResponse(access, refresh, jwt.expiresIn(), user.getId(), tenant.getId(),
            tenant.getName(), user.getFirstName(), user.getLastName(), roles, permissions);
    }

    private String generateRefresh() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}