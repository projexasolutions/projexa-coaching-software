package com.projexa.coaching.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PasswordResetService {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final JavaMailSender mail;
    private final SecureRandom random = new SecureRandom();
    private final String mailFrom;
    private final String resetUrl;
    private final String smtpHost;

    public PasswordResetService(JdbcTemplate db, PasswordEncoder encoder,
            ObjectProvider<JavaMailSender> provider,
            @Value("${app.mail.from}") String mailFrom,
            @Value("${app.password-reset.url}") String resetUrl,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.db = db;
        this.encoder = encoder;
        this.mail = provider.getIfAvailable();
        this.mailFrom = mailFrom;
        this.resetUrl = resetUrl;
        this.smtpHost = smtpHost;
    }

    @Transactional
    public void request(String rawSlug, String rawEmail) {
        String slug = rawSlug == null ? "" : rawSlug.trim().toLowerCase(Locale.ROOT);
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (slug.isBlank() || email.isBlank() || email.length() > 255) return;

        List<UUID> users = db.query("""
            select u.id from users u
            join tenants t on t.id=u.tenant_id
            where lower(t.slug)=? and lower(u.email)=?
              and t.status='ACTIVE' and u.status='ACTIVE'
            """, (rs, row) -> rs.getObject(1, UUID.class), slug, email);
        if (users.size() != 1) return;

        // Apply a per-account cooldown. The public response remains identical for
        // unknown accounts and throttled requests to avoid account enumeration.
        Integer recent = db.queryForObject("""
            select count(*) from password_reset_tokens
            where user_id=? and created_at > now() - interval '15 minutes'
            """, Integer.class, users.get(0));
        if (recent != null && recent > 0) return;
        if (smtpHost == null || smtpHost.isBlank() || mail == null) return;

        UUID userId = users.get(0);
        String token = newToken();
        db.update("update password_reset_tokens set consumed_at=now() where user_id=? and consumed_at is null", userId);
        db.update("""
            insert into password_reset_tokens(user_id,token_hash,expires_at)
            values(?,?,now() + interval '30 minutes')
            """, userId, hash(token));
        try {
            MimeMessage message = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Reset your Projexa Coaching OS password");
            helper.setText("A password reset was requested for your Projexa account.\n\n"
                + "Use this one-time link within 30 minutes:\n"
                + resetUrl + "?token=" + token
                + "\n\nIf you did not request this, ignore this email. Your password will not change.", false);
            mail.send(message);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Password reset email could not be sent. Please try again later.", ex);
        }
    }

    @Transactional
    public void reset(String rawToken, String rawPassword) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200)
            throw new IllegalArgumentException("Password reset link is invalid or expired.");
        if (rawPassword == null || rawPassword.length() < 12 || rawPassword.length() > 72)
            throw new IllegalArgumentException("Password must contain between 12 and 72 characters.");

        var rows = db.query("""
            select id,user_id from password_reset_tokens
            where token_hash=? and consumed_at is null and expires_at>now()
            for update
            """, (rs, row) -> new ResetToken(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class)),
            hash(rawToken));
        if (rows.isEmpty()) throw new IllegalArgumentException("Password reset link is invalid or expired.");
        ResetToken token = rows.get(0);
        int changed = db.update("""
            update users set password_hash=?, updated_at=now()
            where id=? and status='ACTIVE'
            """, encoder.encode(rawPassword), token.userId());
        if (changed != 1) throw new IllegalArgumentException("Password reset link is invalid or expired.");
        db.update("update password_reset_tokens set consumed_at=now() where user_id=? and consumed_at is null", token.userId());
        db.update("update refresh_tokens set revoked_at=now(),last_used_at=now() where user_id=? and revoked_at is null", token.userId());
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record ResetToken(UUID id, UUID userId) {}
}
