package com.projexa.coaching.auth.controller;

import com.projexa.coaching.auth.service.AccountInvitationService;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
public class AccountInvitationController {
    private final AccountInvitationService invitations;
    public AccountInvitationController(AccountInvitationService invitations){this.invitations=invitations;}

    @PostMapping
    @PreAuthorize("hasAuthority('users.manage') or hasAuthority('settings.manage')")
    public ResponseEntity<Map<String,String>> invite(@Valid @RequestBody InviteRequest request,Authentication auth){
        invitations.invite(TenantContextHolder.getRequired(),UUID.fromString(auth.getName()),request.email(),request.role());
        return ResponseEntity.accepted().body(Map.of("message","Invitation sent. The recipient must accept it before an account is activated."));
    }

    @PostMapping("/accept")
    public ResponseEntity<Map<String,String>> accept(@Valid @RequestBody AcceptRequest request){
        invitations.accept(request.token(),request.password());
        return ResponseEntity.ok(Map.of("message","Invitation accepted. You can now sign in."));
    }

    public record InviteRequest(@NotBlank @Email @Size(max=255) String email,@NotBlank String role){}
    public record AcceptRequest(@NotBlank String token,@NotBlank @Size(min=12,max=72) String password){}
}