package com.futuremessage.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Create a locked message. Omit recipientEmail to send to yourself.")
public record CreateMessageRequest(
        @NotBlank @Size(max = 200)
        @Schema(example = "To future me", maxLength = 200)
        String title,
        @NotBlank @Size(max = 20_000)
        @Schema(example = "Keep going.", maxLength = 20_000)
        String content,
        @NotNull @Future
        @Schema(type = "string", format = "date-time", example = "2030-01-01T00:00:00+07:00")
        Instant unlockAt,
        @Email @Size(max = 320)
        @Schema(example = "bob@example.com", description = "Recipient email. Omit or use your own email to send to self.")
        String recipientEmail
) {
}
