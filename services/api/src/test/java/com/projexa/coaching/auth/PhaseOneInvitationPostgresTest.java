package com.projexa.coaching.auth;

import com.projexa.coaching.auth.service.AccountInvitationService;
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

class PhaseOneInvitationPostgresTest {
    @Test
    void teacherStudentAndParentInvitationsCreateAndLinkPortalAccounts() throws Exception {
        String url = System.getenv("PHASE1_TEST_DATABASE_URL");
        org.junit.jupiter.api.Assumptions.assumeTrue(url != null && !url.isBlank());
        String username = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_USERNAME", "projexa");
        String password = System.getenv().getOrDefault("PHASE1_TEST_DATABASE_PASSWORD", "integration-test-only");
        Flyway.configure().dataSource(url, username, password).locations("classpath:db/migration").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, username, password));

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = db.queryForObject("insert into tenants(name,slug,status) values(?,?,'ACTIVE') returning id",
            UUID.class, "Invitation Test Institute", "invite-" + suffix);
        UUID inviterId = db.queryForObject("""
            insert into users(tenant_id,email,password_hash,first_name,status)
            values(?,?,?,'Institute','ACTIVE') returning id
            """, UUID.class, tenantId, "admin-" + suffix + "@example.test", "not-a-real-password");

        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(call -> new MimeMessage(Session.getInstance(new Properties())));
        doNothing().when(sender).send(any(MimeMessage.class));
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        AccountInvitationService service = new AccountInvitationService(db, new BCryptPasswordEncoder(), provider,
            "no-reply@example.test", "https://app.example.test/accept-invitation", "smtp.example.test", 72);

        String[] roles = {"TEACHER", "STUDENT", "PARENT"};
        String[] tables = {"teachers", "students", "parents"};
        String[] emails = new String[roles.length];
        UUID[] profiles = new UUID[roles.length];
        for (int i = 0; i < roles.length; i++) {
            emails[i] = roles[i].toLowerCase() + "-" + suffix + "@example.test";
            if ("teachers".equals(tables[i])) {
                profiles[i] = db.queryForObject("""
                    insert into teachers(tenant_id,first_name,last_name,email)
                    values(?,'Portal','Teacher',?) returning id
                    """, UUID.class, tenantId, emails[i]);
            } else if ("students".equals(tables[i])) {
                profiles[i] = db.queryForObject("""
                    insert into students(tenant_id,admission_number,first_name,last_name,email)
                    values(?,?,'Portal','Student',?) returning id
                    """, UUID.class, tenantId, "ADM-" + suffix + "-" + i, emails[i]);
            } else {
                profiles[i] = db.queryForObject("""
                    insert into parents(tenant_id,first_name,last_name,email)
                    values(?,'Portal','Parent',?) returning id
                    """, UUID.class, tenantId, emails[i]);
            }
            service.invite(tenantId, inviterId, emails[i], roles[i]);
        }

        var messages = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(3)).send(messages.capture());
        for (int i = 0; i < roles.length; i++) {
            String body = messages.getAllValues().get(i).getContent().toString();
            var matcher = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)").matcher(body);
            assertTrue(matcher.find(), "Invitation email must contain a raw single-use token");
            service.accept(matcher.group(1), "new-portal-password-123");
            assertEquals(1, db.queryForObject("select count(*) from " + tables[i] +
                " where id=? and tenant_id=? and user_id is not null", Integer.class, profiles[i], tenantId));
            assertEquals(1, db.queryForObject("select count(*) from users where tenant_id=? and lower(email)=? and status='ACTIVE'",
                Integer.class, tenantId, emails[i]));
            assertEquals(1, db.queryForObject("""
                select count(*) from account_invitations
                where tenant_id=? and lower(email)=? and accepted_at is not null
                """, Integer.class, tenantId, emails[i]));
        }
        assertThrows(IllegalArgumentException.class,
            () -> service.accept(Pattern.compile("[?&]token=([A-Za-z0-9_-]+)")
                .matcher(messages.getAllValues().get(0).getContent().toString()).replaceAll("$1"), "new-portal-password-123"));
    }
}
