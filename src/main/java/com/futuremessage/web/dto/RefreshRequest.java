package com.futuremessage.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Rotate refresh token and issue a new access token")
public record RefreshRequest(
        @NotBlank
        @Schema(example = "paste-refresh-token-from-login-or-register")
        String refreshToken
) {
}
