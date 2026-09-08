package com.futuremessage.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Partial update. Only allowed while the message is LOCKED and you are the sender.")
public record UpdateMessageRequest(
        @Size(min = 1, max = 200)
        @Schema(example = "Updated title", maxLength = 200)
        String title,
        @Size(min = 1, max = 20_000)
        @Schema(example = "Updated content", maxLength = 20_000)
        String content,
        @Future
        @Schema(type = "string", format = "date-time", example = "2031-01-01T00:00:00+07:00")
        Instant unlockAt
) {
}
