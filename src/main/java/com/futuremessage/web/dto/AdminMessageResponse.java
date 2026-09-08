package com.futuremessage.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.futuremessage.domain.MessageStatus;
import com.futuremessage.domain.NotificationStatus;
import com.futuremessage.domain.RecipientType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Admin message detail. content is omitted while LOCKED or CANCELLED.")
public record AdminMessageResponse(
        UUID id,
        UUID senderId,
        String senderEmail,
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
