package com.projexa.coaching.auth.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.*;

@Service
public class AccountInvitationService {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final JavaMailSender mail;
    private final SecureRandom random = new SecureRandom();
    private final String mailFrom;
    private final String acceptUrl;
    private final String smtpHost;
    private final int expiryMinutes;

    public AccountInvitationService(JdbcTemplate db, PasswordEncoder encoder,
            ObjectProvider<JavaMailSender> provider,
            @Value("${app.mail.from}") String mailFrom,
            @Value("${app.invitation.accept-url}") String acceptUrl,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.invitation.expiry-minutes:72}") int expiryMinutes) {
        this.db=db; this.encoder=encoder; this.mail=provider.getIfAvailable();
        this.mailFrom=mailFrom; this.acceptUrl=acceptUrl; this.smtpHost=smtpHost;
        this.expiryMinutes=Math.max(15,Math.min(expiryMinutes,10080));
    }

    @Transactional
    public void invite(UUID tenantId, UUID inviterId, String rawEmail, String rawRole) {
        if (smtpHost == null || smtpHost.isBlank() || mail == null)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Invitation email delivery is not configured.");
        String email=rawEmail==null?"":rawEmail.trim().toLowerCase(Locale.ROOT);
        String role=rawRole==null?"":rawRole.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("TEACHER","STUDENT","PARENT").contains(role)) throw new IllegalArgumentException("Choose a teacher, student or parent role.");
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) throw new IllegalArgumentException("Enter a valid email address.");
        if (count("select count(*) from users where tenant_id=? and lower(email)=?",tenantId,email)>0)
            throw new IllegalArgumentException("An account with this email already exists.");
        String table=profileTable(role);
        if (count("select count(*) from "+table+" where tenant_id=? and lower(email)=?",tenantId,email)!=1)
            throw new IllegalArgumentException("Exactly one matching "+role.toLowerCase(Locale.ROOT)+" profile must exist in this institute before inviting this email.");
        String token=newToken();
        db.update("insert into account_invitations(tenant_id,email,role_code,token_hash,expires_at,created_by) values(?,?,?,?,?,?)",
            tenantId,email,role,hash(token),LocalDateTime.now().plusMinutes(expiryMinutes),inviterId);
        sendInvitationEmail(email,role,token);
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> list(UUID tenantId) {
        return db.queryForList("""
            select id,email,role_code,expires_at,accepted_at,revoked_at,created_at
            from account_invitations
            where tenant_id=? and revoked_at is null and accepted_at is null and expires_at>now()
            order by created_at desc
            """, tenantId);
    }

    @Transactional
    public void revoke(UUID tenantId, UUID invitationId) {
        int changed = db.update("""
            update account_invitations set revoked_at=now()
            where id=? and tenant_id=? and accepted_at is null and revoked_at is null
            """, invitationId, tenantId);
        if (changed != 1) throw new IllegalArgumentException("Invitation is no longer pending or does not exist.");
    }

    @Transactional
    public void resend(UUID tenantId, UUID invitationId) {
        if (smtpHost == null || smtpHost.isBlank() || mail == null)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Invitation email delivery is not configured.");
        var rows = db.query("""
            select id,email,role_code from account_invitations
            where id=? and tenant_id=? and accepted_at is null and revoked_at is null
            for update
            """, (rs,n) -> new Invitation(rs.getObject("id",UUID.class),tenantId,rs.getString("email"),rs.getString("role_code")), invitationId,tenantId);
        if(rows.isEmpty()) throw new IllegalArgumentException("Invitation is no longer pending or does not exist.");
        Invitation invitation=rows.get(0);
        String token=newToken();
        int updated=db.update("""
            update account_invitations set token_hash=?,expires_at=?,created_at=now()
            where id=? and tenant_id=? and accepted_at is null and revoked_at is null
            """,hash(token),LocalDateTime.now().plusMinutes(expiryMinutes),invitationId,tenantId);
        if(updated!=1) throw new IllegalArgumentException("Invitation could not be renewed.");
        sendInvitationEmail(invitation.email(),invitation.role(),token);
    }

    private void sendInvitationEmail(String email,String role,String token) {
        try {
            MimeMessage message=mail.createMimeMessage();
            MimeMessageHelper helper=new MimeMessageHelper(message,StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom); helper.setTo(email);
            helper.setSubject("You are invited to Projexa Coaching OS");
            helper.setText("You have been invited to access your institute as a "+role.toLowerCase(Locale.ROOT)+".\\n\\n"+
                "Accept the invitation and create your password using this link (expires in "+expiryMinutes+" minutes):\\n"+
                acceptUrl+"?token="+token+"\\n\\nIf you were not expecting this invitation, ignore this email.",false);
            mail.send(message);
        } catch(Exception ex) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Invitation email could not be sent. Please try again.",ex);
        }
    }

    @Transactional
    public void accept(String rawToken, String rawPassword) {
        if(rawToken==null||rawToken.isBlank()||rawToken.length()>200) throw new IllegalArgumentException("Invitation is invalid or expired.");
        if(rawPassword==null||rawPassword.length()<12||rawPassword.length()>72) throw new IllegalArgumentException("Password must contain between 12 and 72 characters.");
        var rows=db.query("""
            select id,tenant_id,email,role_code from account_invitations
            where token_hash=? and accepted_at is null and expires_at>now()
            for update
            """,(rs,n)->new Invitation(rs.getObject("id",UUID.class),rs.getObject("tenant_id",UUID.class),rs.getString("email"),rs.getString("role_code")),hash(rawToken));
        if(rows.isEmpty()) throw new IllegalArgumentException("Invitation is invalid or expired.");
        Invitation invitation=rows.get(0);
        String table=profileTable(invitation.role());
        var profiles=db.query("select id,first_name,last_name from "+table+" where tenant_id=? and lower(email)=?",
            (rs,n)->new Profile(rs.getObject("id",UUID.class),rs.getString("first_name"),rs.getString("last_name")),invitation.tenantId(),invitation.email());
        if(profiles.size()!=1) throw new IllegalArgumentException("The matching institute profile is no longer available.");
        if(count("select count(*) from users where tenant_id=? and lower(email)=?",invitation.tenantId(),invitation.email())>0)
            throw new IllegalArgumentException("An account with this email already exists.");
        Profile profile=profiles.get(0);
        UUID userId=db.queryForObject("""
            insert into users(tenant_id,email,password_hash,first_name,last_name,status)
            values(?,?,?,?,?,'ACTIVE') returning id
            """,UUID.class,invitation.tenantId(),invitation.email(),encoder.encode(rawPassword),profile.firstName(),profile.lastName());
        UUID roleId=db.query("""
            insert into roles(tenant_id,name,code,description,system_role)
            values(?,?,?,?,true)
            on conflict do nothing
            returning id
            """,(rs,n)->rs.getObject(1,UUID.class),invitation.tenantId(),roleName(invitation.role()),invitation.role(),"Institute portal role").stream().findFirst().orElseGet(()->db.queryForObject("select id from roles where tenant_id=? and code=?",UUID.class,invitation.tenantId(),invitation.role()));
        db.update("insert into user_roles(user_id,role_id) values(?,?)",userId,roleId);
        db.update("update "+table+" set user_id=? where id=? and tenant_id=?",userId,profile.id(),invitation.tenantId());
        db.update("update account_invitations set accepted_at=now() where id=?",invitation.id());
    }

    private String profileTable(String role){return switch(role){case "TEACHER"->"teachers";case "STUDENT"->"students";case "PARENT"->"parents";default->throw new IllegalArgumentException("Unsupported invitation role");};}
    private String roleName(String role){return switch(role){case "TEACHER"->"Teacher";case "STUDENT"->"Student";case "PARENT"->"Parent";default->role;};}
    private int count(String sql,Object...args){Integer n=db.queryForObject(sql,Integer.class,args);return n==null?0:n;}
    private String newToken(){byte[] b=new byte[32];random.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    private record Invitation(UUID id,UUID tenantId,String email,String role){}
    private record Profile(UUID id,String firstName,String lastName){}
}