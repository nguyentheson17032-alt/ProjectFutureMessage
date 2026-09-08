package com.futuremessage.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Create a new account")
public record RegisterRequest(
        @NotBlank @Email @Size(max = 320)
        @Schema(example = "ada@example.com", description = "Unique login email")
        String email,
        @NotBlank @Size(min = 8, max = 72)
        @Schema(example = "password1", format = "password", minLength = 8, maxLength = 72)
        String password,
        @NotBlank @Size(max = 100)
        @Schema(example = "Ada", maxLength = 100)
        String displayName
) {
}
