package com.projexa.coaching.auth.service;

import com.projexa.coaching.auth.security.JwtService;
import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.tenancy.repository.TenantRepository;
import com.projexa.coaching.users.entity.RefreshToken;
import com.projexa.coaching.users.entity.User;
import com.projexa.coaching.users.repository.RefreshTokenRepository;
import com.projexa.coaching.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuthService service = new AuthService(tenants, users, refreshTokens, encoder, jwt);

    @Test
    void inactiveUserCannotRefreshAccessAndNoNewTokenIsIssued() {
        RefreshToken stored = mock(RefreshToken.class);
        User inactive = mock(User.class);
        when(stored.getExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
        when(stored.getUserId()).thenReturn(UUID.randomUUID());
        when(refreshTokens.findByTokenHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.of(stored));
        when(users.findById(any(UUID.class))).thenReturn(Optional.of(inactive));
        when(inactive.getStatus()).thenReturn("DISABLED");

        ApiException error = assertThrows(ApiException.class, () -> service.refresh("valid-looking-refresh-token"));

        assertEquals("USER_INACTIVE", error.getCode());
        verify(refreshTokens, never()).save(any(RefreshToken.class));
        verifyNoInteractions(tenants, jwt);
    }

    @Test
    void logoutLooksUpOnlyThePresentedTokenInsteadOfLoadingEveryToken() {
        when(refreshTokens.findByTokenHashAndRevokedAtIsNull(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.logout("unknown-token"));

        verify(refreshTokens).findByTokenHashAndRevokedAtIsNull(anyString());
        verifyNoMoreInteractions(refreshTokens);
    }

    @Test
    void blankLogoutIsSafeAndDoesNotQueryTheDatabase() {
        assertDoesNotThrow(() -> service.logout("  "));
        verifyNoInteractions(refreshTokens);
    }
}