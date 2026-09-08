package com.futuremessage.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Sign in with an existing account")
public record LoginRequest(
        @NotBlank @Email @Size(max = 320)
        @Schema(example = "ada@example.com")
        String email,
        @NotBlank
        @Schema(example = "password1", format = "password")
        String password
) {
}
