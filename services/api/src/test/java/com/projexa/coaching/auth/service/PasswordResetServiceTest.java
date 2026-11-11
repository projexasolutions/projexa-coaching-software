package com.projexa.coaching.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private ObjectProvider<JavaMailSender> provider() {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(mail);
        return p;
    }

    @Test
    void blankRequestDoesNotQueryDatabase() {
        var service = new PasswordResetService(db, encoder, provider(), "no-reply@example.test",
            "https://example.test/reset", "smtp.example.test");
        assertDoesNotThrow(() -> service.request(" ", " "));
        verifyNoInteractions(db, encoder, mail);
    }

    @Test
    void blankTokenIsRejectedBeforeDatabaseAccess() {
        var service = new PasswordResetService(db, encoder, provider(), "no-reply@example.test",
            "https://example.test/reset", "smtp.example.test");
        assertThrows(IllegalArgumentException.class, () -> service.reset(" ", "a-secure-password"));
        verifyNoInteractions(db, encoder, mail);
    }

    @Test
    void shortPasswordIsRejectedBeforeDatabaseAccess() {
        var service = new PasswordResetService(db, encoder, provider(), "no-reply@example.test",
            "https://example.test/reset", "smtp.example.test");
        assertThrows(IllegalArgumentException.class, () -> service.reset("token", "short"));
        verifyNoInteractions(db, encoder, mail);
    }
}
