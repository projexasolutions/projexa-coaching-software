package com.projexa.coaching.auth;

import com.projexa.coaching.auth.service.PasswordResetService;
import jakarta.mail.Session;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PhaseOnePasswordResetPostgresTest {
    @Test
    void passwordResetConsumesTokenAndRevokesAllRefreshSessions() throws Exception {
        String url = System.getenv("PHASE1_TEST_DATABASE_URL");
        org.junit.jupiter.api.Assumptions.assumeTrue(url != null && !url.isBlank());
        String username = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_USERNAME", "projexa");
        String password = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_PASSWORD", "integration-test-only");
        Flyway.configure().dataSource(url, username, password).locations("classpath:db/migration").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, username, password));

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = "reset-" + suffix;
        String email = "reset-" + suffix + "@example.test";
        UUID tenantId = db.queryForObject("insert into tenants(name,slug,status) values(?,?,'ACTIVE') returning id",
            UUID.class, "Password Reset Test Institute", slug);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UUID userId = db.queryForObject("""
            insert into users(tenant_id,email,password_hash,first_name,status)
            values(?,?,?,?, 'ACTIVE') returning id
            """, UUID.class, tenantId, email, encoder.encode("old-password-123"), "Reset");

        db.update("""
            insert into refresh_tokens(user_id,token_hash,expires_at)
            values(?,?,now() + interval '7 days')
            """, userId, "existing-session-hash");

        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        doNothing().when(sender).send(message);
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        PasswordResetService service = new PasswordResetService(db, encoder, provider,
            "no-reply@example.test", "https://app.example.test/reset-password", "smtp.example.test");

        service.request(slug, email);
        String body = message.getContent().toString();
        var matcher = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)").matcher(body);
        assertTrue(matcher.find(), "Password reset email must contain a raw one-time token");
        String token = matcher.group(1);

        service.reset(token, "new-secure-password-456");

        String newHash = db.queryForObject("select password_hash from users where id=?", String.class, userId);
        assertTrue(encoder.matches("new-secure-password-456", newHash));
        assertEquals(1, db.queryForObject("select count(*) from refresh_tokens where user_id=? and revoked_at is not null",
            Integer.class, userId));
        assertEquals(1, db.queryForObject("select count(*) from password_reset_tokens where user_id=? and consumed_at is not null",
            Integer.class, userId));
        assertThrows(IllegalArgumentException.class, () -> service.reset(token, "another-secure-password-789"));
        verify(sender, times(1)).send(message);
    }
}
