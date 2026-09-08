package com.futuremessage.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Invalidate a refresh token")
public record LogoutRequest(
        @NotBlank
        @Schema(example = "paste-refresh-token-from-login-or-register")
        String refreshToken
) {
}
