package com.projexa.coaching.auth;

import com.projexa.coaching.auth.service.InstituteSignupService;
import jakarta.mail.internet.MimeMessage;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.mail.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PhaseOneSignupPostgresTest {
    @Test
    void instituteSignupEmailVerificationActivatesTenantAndOwnerExactlyOnce() throws Exception {
        String url = System.getenv("PHASE1_TEST_DATABASE_URL");
        org.junit.jupiter.api.Assumptions.assumeTrue(url != null && !url.isBlank());
        String username = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_USERNAME", "projexa");
        String password = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_PASSWORD", "integration-test-only");

        Flyway.configure().dataSource(url, username, password).locations("classpath:db/migration").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, username, password));

        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        doNothing().when(sender).send(message);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);

        InstituteSignupService service = new InstituteSignupService(db, new BCryptPasswordEncoder(), provider,
            "no-reply@example.test", "https://app.example.test/verify-institute", "smtp.example.test", 30);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = "phase1-" + suffix;
        String email = "owner-" + suffix + "@example.test";
        service.signup(new InstituteSignupService.SignupRequest(
            "Phase One Test Institute", slug, "Test", "Owner", email, "strong-password-123"));

        String emailBody = message.getContent().toString();
        var matcher = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)").matcher(emailBody);
        assertTrue(matcher.find(), "Verification email must contain a raw single-use token");
        String token = matcher.group(1);

        assertEquals("PENDING_VERIFICATION", db.queryForObject(
            "select status from tenants where slug=?", String.class, slug));
        assertEquals("PENDING_VERIFICATION", db.queryForObject(
            "select status from users where tenant_id=(select id from tenants where slug=?) and lower(email)=?",
            String.class, slug, email));

        service.verify(token);

        assertEquals("ACTIVE", db.queryForObject(
            "select status from tenants where slug=?", String.class, slug));
        assertEquals("ACTIVE", db.queryForObject(
            "select status from users where tenant_id=(select id from tenants where slug=?) and lower(email)=?",
            String.class, slug, email));
        assertEquals(1, db.queryForObject(
            "select count(*) from institute_signup_verifications where token_hash is not null and consumed_at is not null and user_id=(select id from users where lower(email)=?)",
            Integer.class, email));
        assertThrows(IllegalArgumentException.class, () -> service.verify(token));
        verify(sender, times(1)).send(message);
    }
}
