package com.projexa.coaching.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class InstituteSignupService {
    private static final Pattern SLUG = Pattern.compile("[a-z0-9](?:[a-z0-9-]{1,58}[a-z0-9])?");
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final JavaMailSender mail;
    private final SecureRandom random = new SecureRandom();
    private final String mailFrom;
    private final String verificationUrl;
    private final String smtpHost;
    private final int expiryMinutes;

    public InstituteSignupService(JdbcTemplate db, PasswordEncoder encoder, ObjectProvider<JavaMailSender> mailProvider,
            @Value("${app.mail.from}") String mailFrom,
            @Value("${app.signup.verification-url}") String verificationUrl,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.signup.verification-minutes:30}") int expiryMinutes) {
        this.db = db;
        this.encoder = encoder;
        this.mail = mailProvider.getIfAvailable();
        this.mailFrom = mailFrom;
        this.verificationUrl = verificationUrl;
        this.smtpHost = smtpHost;
        this.expiryMinutes = Math.max(5, Math.min(expiryMinutes, 120));
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (smtpHost == null || smtpHost.isBlank() || mail == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Institute signup is temporarily unavailable because email delivery is not configured.");
        }
        String institute = request.instituteName().trim();
        String slug = request.instituteSlug().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (!SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException("Institute slug must be 3–60 characters and contain only lowercase letters, numbers and hyphens.");
        }
        if (request.password().length() < 12 || request.password().length() > 72) {
            throw new IllegalArgumentException("Password must contain between 12 and 72 characters.");
        }
        if (institute.length() > 150 || request.firstName().trim().length() > 100 ||
                (request.lastName() != null && request.lastName().trim().length() > 100)) {
            throw new IllegalArgumentException("One or more signup fields exceed the allowed length.");
        }
        Integer slugCount = db.queryForObject("select count(*) from tenants where lower(slug)=?", Integer.class, slug);
        if (slugCount != null && slugCount > 0) throw new IllegalArgumentException("This institute URL is already taken.");
        Integer emailCount = db.queryForObject("select count(*) from users where lower(email)=?", Integer.class, email);
        if (emailCount != null && emailCount > 0) throw new IllegalArgumentException("An account with this email already exists.");

        UUID tenantId = db.queryForObject("""
            insert into tenants(name,slug,status,timezone,contact_email)
            values(?,?,'PENDING_VERIFICATION','Asia/Kolkata',?)
            returning id
            """, UUID.class, institute, slug, email);
        UUID userId = db.queryForObject("""
            insert into users(tenant_id,email,password_hash,first_name,last_name,status)
            values(?,?,?,?,?,'PENDING_VERIFICATION')
            returning id
            """, UUID.class, tenantId, email, encoder.encode(request.password()),
                request.firstName().trim(), request.lastName() == null ? null : request.lastName().trim());

        UUID ownerRole = db.queryForObject("""
            insert into roles(tenant_id,name,code,description,system_role)
            values(?,'Institute Owner','INSTITUTE_OWNER','Institute owner account',true)
            returning id
            """, UUID.class, tenantId);
        db.update("""
            insert into role_permissions(role_id,permission_id)
            select ?,id from permissions where code not like 'platform.%'
            on conflict do nothing
            """, ownerRole);
        db.update("insert into user_roles(user_id,role_id) values(?,?)", userId, ownerRole);

        String token = newToken();
        db.update("""
            insert into institute_signup_verifications(tenant_id,user_id,token_hash,expires_at)
            values(?,?,?,?)
            """, tenantId, userId, hash(token), LocalDateTime.now().plusMinutes(expiryMinutes));

        try {
            MimeMessage message = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("Verify your Projexa Coaching OS institute account");
            helper.setText("Hello " + request.firstName().trim() + ",\n\n" +
                "Verify your institute account using this secure link (valid for " + expiryMinutes + " minutes):\n" +
                verificationUrl + "?token=" + token + "\n\nIf you did not request this account, you can ignore this email.", false);
            mail.send(message);
        } catch (Exception ex) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "We could not send the verification email. Please try again later.", ex);
        }
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) {
            throw new IllegalArgumentException("Verification link is invalid or expired.");
        }
        var rows = db.query("""
            select id,tenant_id,user_id from institute_signup_verifications
            where token_hash=? and consumed_at is null and expires_at>now()
            for update
            """, (rs, row) -> new Verification(rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class), rs.getObject("user_id", UUID.class)), hash(rawToken));
        if (rows.isEmpty()) throw new IllegalArgumentException("Verification link is invalid or expired.");
        Verification verification = rows.get(0);
        db.update("update users set status='ACTIVE',updated_at=now() where id=? and tenant_id=? and status='PENDING_VERIFICATION'",
            verification.userId(), verification.tenantId());
        int activated = db.update("update tenants set status='ACTIVE',updated_at=now() where id=? and status='PENDING_VERIFICATION'",
            verification.tenantId());
        if (activated != 1) throw new IllegalArgumentException("Institute account could not be activated.");
        db.update("update institute_signup_verifications set consumed_at=now() where id=?", verification.id());
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

    public record SignupRequest(String instituteName, String instituteSlug, String firstName,
                                String lastName, String email, String password) {}
    private record Verification(UUID id, UUID tenantId, UUID userId) {}
}