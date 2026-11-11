package com.projexa.coaching.auth.controller;

import com.projexa.coaching.auth.dto.*;
import com.projexa.coaching.auth.service.AuthService;
import com.projexa.coaching.auth.service.InstituteSignupService;
import com.projexa.coaching.common.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final InstituteSignupService signup;

    public AuthController(AuthService auth, InstituteSignupService signup) {
        this.auth = auth;
        this.signup = signup;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest req) {
        return ApiResponse.of(auth.login(request), requestId(req));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest req) {
        return ApiResponse.of(auth.refresh(request.refreshToken()), requestId(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        auth.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupPayload request) {
        signup.signup(new InstituteSignupService.SignupRequest(request.instituteName(), request.instituteSlug(),
            request.firstName(), request.lastName(), request.email(), request.password()));
        return ResponseEntity.accepted().body(Map.of("message",
            "Signup received. Check your email for a verification link before signing in."));
    }

    @PostMapping("/verify-institute")
    public ResponseEntity<Map<String, String>> verify(@RequestBody VerificationPayload request) {
        signup.verify(request.token());
        return ResponseEntity.ok(Map.of("message", "Institute account verified. You can now sign in."));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(org.springframework.security.core.Authentication authentication,
                                                HttpServletRequest req) {
        return ApiResponse.of(Map.of("userId", authentication.getPrincipal(),
            "authorities", authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList()), requestId(req));
    }

    private String requestId(HttpServletRequest request) {
        return request.getAttribute("requestId") instanceof String value ? value : null;
    }

    public record SignupPayload(@NotBlank @Size(max=150) String instituteName,
        @NotBlank @Size(min=3,max=60) String instituteSlug,
        @NotBlank @Size(max=100) String firstName, @Size(max=100) String lastName,
        @NotBlank @Email @Size(max=255) String email,
        @NotBlank @Size(min=12,max=72) String password) {}
    public record VerificationPayload(@NotBlank String token) {}
}