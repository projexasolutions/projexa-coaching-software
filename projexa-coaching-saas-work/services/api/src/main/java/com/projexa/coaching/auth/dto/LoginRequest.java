package com.projexa.coaching.auth.dto; import jakarta.validation.constraints.*; public record LoginRequest(@NotBlank String tenantSlug,@NotBlank @Email String email,@NotBlank String password){}
