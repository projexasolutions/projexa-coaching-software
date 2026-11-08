package com.projexa.coaching.auth.controller;

import com.projexa.coaching.auth.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
public class PasswordResetController {
    private final PasswordResetService resets;

    public PasswordResetController(PasswordResetService resets) {
        this.resets = resets;
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> request(@Valid @RequestBody RequestPayload payload) {
        resets.request(payload.tenantSlug(), payload.email());
        return ResponseEntity.accepted().body(Map.of("message",
            "If an active account matches those details, a password reset link will be sent."));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@Valid @RequestBody ConfirmPayload payload) {
        resets.reset(payload.token(), payload.password());
        return ResponseEntity.ok(Map.of("message", "Password updated. Sign in with your new password."));
    }

    public record RequestPayload(@NotBlank @Size(max=60) String tenantSlug,
                                 @NotBlank @Size(max=255) String email) {}
    public record ConfirmPayload(@NotBlank String token,
                                 @NotBlank @Size(min=12,max=72) String password) {}
}
