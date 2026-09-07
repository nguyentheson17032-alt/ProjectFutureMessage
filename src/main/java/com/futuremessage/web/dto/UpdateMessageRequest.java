package com.futuremessage.web.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateMessageRequest(
        @Size(min = 1, max = 200) String title,
        @Size(min = 1, max = 20_000) String content,
        @Future Instant unlockAt
) {
}
