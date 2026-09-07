package com.futuremessage.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateMessageRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20_000) String content,
        @NotNull @Future Instant unlockAt,
        @Email @Size(max = 320) String recipientEmail
) {
}
