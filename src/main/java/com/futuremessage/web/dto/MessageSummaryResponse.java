package com.futuremessage.web.dto;

import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.RecipientType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

public record MessageSummaryResponse(
        UUID id,
        UUID senderId,
        String senderDisplayName,
        String recipientEmail,
        RecipientType recipientType,
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) String content,
        Instant unlockAt,
        MessageStatus status,
        Instant openedAt,
        Instant createdAt
) {
}
