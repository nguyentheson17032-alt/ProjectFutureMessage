package com.futuremessage.web.dto;

import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        String senderDisplayName,
        String recipientEmail,
        UUID recipientUserId,
        RecipientType recipientType,
        String title,
        @JsonInclude(JsonInclude.Include.NON_NULL) String content,
        Instant unlockAt,
        MessageStatus status,
        Instant openedAt,
        NotificationStatus notificationStatus,
        Instant notifiedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
