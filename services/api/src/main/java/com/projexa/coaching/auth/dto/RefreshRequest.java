package com.projexa.coaching.auth.dto; import jakarta.validation.constraints.NotBlank; public record RefreshRequest(@NotBlank String refreshToken){}
