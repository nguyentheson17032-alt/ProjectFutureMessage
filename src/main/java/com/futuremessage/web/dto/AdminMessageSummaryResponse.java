package com.futuremessage.web.dto;

import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Admin message list row: metadata only, never content.")
public record AdminMessageSummaryResponse(
        UUID id,
        UUID senderId,
        String senderEmail,
        String senderDisplayName,
        String recipientEmail,
        UUID recipientUserId,
        RecipientType recipientType,
        String title,
        Instant unlockAt,
        MessageStatus status,
        Instant openedAt,
        NotificationStatus notificationStatus,
        Instant notifiedAt,
        Instant createdAt
) {
}
