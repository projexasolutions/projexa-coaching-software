package com.projexa.coaching.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstituteSignupServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);

    private ObjectProvider<JavaMailSender> provider() { ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class); when(provider.getIfAvailable()).thenReturn(mail); return provider; }

    @Test
    void signupFailsClosedWhenSmtpIsNotConfigured() {
        InstituteSignupService service = new InstituteSignupService(db, encoder, provider(),
            "no-reply@example.test", "https://example.test/verify", "", 30);
        var request = new InstituteSignupService.SignupRequest("Institute", "institute-1",
            "Owner", "Test", "owner@example.test", "a-long-enough-password");

        org.springframework.web.server.ResponseStatusException error = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.signup(request));

        assertTrue(error.getReason().contains("email delivery is not configured"));
        verifyNoInteractions(db, encoder, mail);
    }

    @Test
    void verificationRejectsMissingOrBlankTokenWithoutDatabaseLookup() {
        InstituteSignupService service = new InstituteSignupService(db, encoder, provider(),
            "no-reply@example.test", "https://example.test/verify", "smtp.example.test", 30);

        assertThrows(IllegalArgumentException.class, () -> service.verify(" "));
        verifyNoInteractions(db);
    }
}