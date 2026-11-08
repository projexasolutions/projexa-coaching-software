package com.projexa.coaching.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountInvitationServiceTest {
    private final JdbcTemplate db=mock(JdbcTemplate.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private final JavaMailSender mail=mock(JavaMailSender.class);
    private ObjectProvider<JavaMailSender> provider(){ObjectProvider<JavaMailSender> p=mock(ObjectProvider.class);when(p.getIfAvailable()).thenReturn(mail);return p;}

    @Test
    void invitationFailsClosedWhenSmtpIsNotConfigured(){
        var service=new AccountInvitationService(db,encoder,provider(),"no-reply@example.test","https://example.test/accept","",72);
        var error=assertThrows(org.springframework.web.server.ResponseStatusException.class,
            ()->service.invite(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),"teacher@example.test","TEACHER"));
        assertEquals(503,error.getStatusCode().value());
        verifyNoInteractions(db,mail);
    }

    @Test
    void acceptanceRejectsBlankTokenBeforeDatabaseAccess(){
        var service=new AccountInvitationService(db,encoder,provider(),"no-reply@example.test","https://example.test/accept","smtp.example.test",72);
        assertThrows(IllegalArgumentException.class,()->service.accept(" ","a-secure-password"));
        verifyNoInteractions(db,encoder,mail);
    }

    @Test
    void acceptanceRejectsShortPasswordBeforeDatabaseAccess(){
        var service=new AccountInvitationService(db,encoder,provider(),"no-reply@example.test","https://example.test/accept","smtp.example.test",72);
        assertThrows(IllegalArgumentException.class,()->service.accept("token","short"));
        verifyNoInteractions(db,encoder,mail);
    }
}