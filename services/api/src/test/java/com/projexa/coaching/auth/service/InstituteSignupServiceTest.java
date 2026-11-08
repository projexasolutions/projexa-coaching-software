package com.projexa.coaching.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstituteSignupServiceTest {
    private final JdbcTemplate db = mock(JdbcTemplate.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);

    @Test
    void signupFailsClosedWhenSmtpIsNotConfigured() {
        InstituteSignupService service = new InstituteSignupService(db, encoder, mail,
            "no-reply@example.test", "https://example.test/verify", "", 30);
        var request = new InstituteSignupService.SignupRequest("Institute", "institute-1",
            "Owner", "Test", "owner@example.test", "a-long-enough-password");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.signup(request));

        assertTrue(error.getMessage().contains("email delivery is not configured"));
        verifyNoInteractions(db, encoder, mail);
    }

    @Test
    void verificationRejectsMissingOrBlankTokenWithoutDatabaseLookup() {
        InstituteSignupService service = new InstituteSignupService(db, encoder, mail,
            "no-reply@example.test", "https://example.test/verify", "smtp.example.test", 30);

        assertThrows(IllegalArgumentException.class, () -> service.verify(" "));
        verifyNoInteractions(db);
    }
}